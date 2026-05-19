package network;

import model.*;
import util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class GameServer {

    // ── Lobby state ──────────────────────────────────────────
    private final ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private volatile boolean gameStarted = false;
    private volatile boolean gameOver = false;

    // ── Game state (authoritative) ───────────────────────────
    private Maze maze;
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final ScheduledExecutorService gameLoop = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> loopHandle;

    // ── Timer ────────────────────────────────────────────────
    private int timeLeftTicks;
    private int eatMultiplier = 1;

    public GameServer(int port) throws IOException {
        // Bind to 0.0.0.0 so ALL network interfaces (WiFi, Ethernet, etc.) are listened on.
        // Without this, on some systems only loopback is reachable.
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port), 50);
        System.out.println("[Server] Bound to 0.0.0.0:" + port);
        System.out.println("[Server] LAN IP (share this): " + NetworkUtils.getLocalIP());
        NetworkUtils.printDiagnostic();
    }

    public void acceptLoop() {
        System.out.println("[Server] acceptLoop started — ready for connections");
        while (!serverSocket.isClosed()) {
            try {
                Socket sock = serverSocket.accept();
                System.out.println("[Server] Incoming connection from " + sock.getInetAddress().getHostAddress());

                if (gameStarted) {
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true);
                    Map<String,Object> msg = new LinkedHashMap<>();
                    msg.put("type","ERROR");
                    msg.put("reason","Game already in progress");
                    pw.println(NetworkUtils.toJson(msg));
                    sock.close();
                    continue;
                }
                if (clients.size() >= GameConfig.MAX_PLAYERS) {
                    sock.close();
                    continue;
                }
                ClientHandler ch = new ClientHandler(sock, this, nextId.getAndIncrement());
                clients.add(ch);
                new Thread(ch, "Client-" + ch.getId()).start();
                System.out.println("[Server] Player " + ch.getId() + " connected from " + sock.getInetAddress().getHostAddress());
            } catch (IOException e) {
                if (!serverSocket.isClosed()) e.printStackTrace();
            }
        }
    }

    // ── Called by ClientHandler when JOIN received ───────────

    synchronized void onJoin(ClientHandler ch) {
        broadcastLobby();
    }

    // ── Called by ClientHandler when ROLE received ───────────

    synchronized void onRoleSelect(ClientHandler ch) {
        broadcastLobby();
    }

    // ── Called by ClientHandler when READY received ──────────

    synchronized void onReady(ClientHandler ch) {
        broadcastLobby();
    }

    // ── Called by host client to start the game ──────────────

    synchronized void onStartGame() {
        if (gameStarted) return;
        if (clients.size() < GameConfig.MIN_PLAYERS) {
            ClientHandler host = clients.isEmpty() ? null : clients.get(0);
            if (host != null) host.send(buildError("Need at least " + GameConfig.MIN_PLAYERS + " players"));
            return;
        }

        // Resolve roles
        List<ClientHandler> wantChomper = new ArrayList<>();
        List<ClientHandler> wantChaser  = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.preferredRole == Role.CHOMPER) wantChomper.add(c);
            else wantChaser.add(c);
        }

        ClientHandler chomperClient;
        List<ClientHandler> chaserClients = new ArrayList<>();

        if (wantChomper.isEmpty()) {
            Collections.shuffle(new ArrayList<>(clients));
            chomperClient = clients.get(0);
            for (int i = 1; i < clients.size(); i++) chaserClients.add(clients.get(i));
        } else {
            Collections.shuffle(wantChomper);
            chomperClient = wantChomper.get(0);
            chaserClients.addAll(wantChaser);
            for (int i = 1; i < wantChomper.size(); i++) chaserClients.add(wantChomper.get(i));
        }

        chomperClient.assignedRole = Role.CHOMPER;
        for (ClientHandler c : chaserClients) c.assignedRole = Role.CHASER;

        for (ClientHandler c : clients) {
            Map<String,Object> msg = new LinkedHashMap<>();
            msg.put("type","ROLE_ASSIGN");
            msg.put("role", c.assignedRole.name());
            c.send(msg);
        }

        // Init game model
        maze = new Maze();
        players.clear();
        eatMultiplier = 1;
        timeLeftTicks = (GameConfig.GAME_DURATION_S * 1000) / GameConfig.TICK_MS;

        int[][] chaserSpawns = {{1,1},{1,13},{13,1},{13,13}};
        Player chomperPlayer = new Player(7, 7, Role.CHOMPER);
        players.put(chomperClient.getId(), chomperPlayer);
        chomperClient.player = chomperPlayer;

        for (int i = 0; i < chaserClients.size(); i++) {
            ClientHandler cc = chaserClients.get(i);
            int[] sp = chaserSpawns[i % chaserSpawns.length];
            Player cp = new Player(sp[0], sp[1], Role.CHASER);
            players.put(cc.getId(), cp);
            cc.player = cp;
        }

        gameStarted = true;
        gameOver = false;

        Map<String,Object> startMsg = new LinkedHashMap<>();
        startMsg.put("type","GAME_START");
        broadcast(startMsg);

        loopHandle = gameLoop.scheduleAtFixedRate(this::tick, 500, GameConfig.TICK_MS, TimeUnit.MILLISECONDS);
        System.out.println("[Server] Game started with " + clients.size() + " players");
    }

    // ── Game loop tick ───────────────────────────────────────

    private synchronized void tick() {
        if (gameOver) return;

        timeLeftTicks--;

        ClientHandler chomperCH = null;
        List<ClientHandler> chaserCHs = new ArrayList<>();

        for (ClientHandler ch : clients) {
            if (!ch.connected) continue;
            if (ch.assignedRole == Role.CHOMPER) chomperCH = ch;
            else chaserCHs.add(ch);
        }

        if (chomperCH == null) {
            endGame("CHASERS", "CHOMPER_LEFT");
            return;
        }

        chomperCH.applyBufferedDirection();
        chomperCH.player.move(maze);

        boolean chomperPowered = chomperCH.player.isPowered();

        for (ClientHandler ch : chaserCHs) {
            if (!ch.connected) continue;
            ch.applyBufferedDirection();
            ch.player.move(maze);
            if (ch.player.collidesWith(chomperCH.player)) {
                if (chomperPowered) {
                    ch.player.respawn();
                    chomperCH.player.addScore(GameConfig.SCORE_EAT_BASE * eatMultiplier);
                    eatMultiplier *= 2;
                } else {
                    chomperCH.player.loseLife();
                    if (!chomperCH.player.isAlive()) {
                        endGame("CHASERS", "NO_LIVES");
                        return;
                    }
                    for (ClientHandler cc : chaserCHs) cc.player.respawn();
                }
            }
        }

        if (maze.countRemainingPellets() == 0) {
            endGame("CHOMPERS", "ALL_PELLETS");
            return;
        }

        if (timeLeftTicks <= 0) {
            endGame("CHASERS", "TIME_UP");
            return;
        }

        broadcastState();
    }

    private void endGame(String winner, String reason) {
        gameOver = true;
        if (loopHandle != null) loopHandle.cancel(false);

        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type", winner.equals("CHOMPERS") ? "WIN" : "GAME_OVER");
        msg.put("winner", winner);
        msg.put("reason", reason);
        broadcast(msg);
        System.out.println("[Server] Game over — winner: " + winner + " (" + reason + ")");
    }

    synchronized void onInput(ClientHandler ch, String direction) {
        if (ch.player == null) return;
        try {
            Direction d = Direction.valueOf(direction);
            ch.bufferedDirection = d;
        } catch (Exception ignored) {}
    }

    synchronized void onDisconnect(ClientHandler ch) {
        clients.remove(ch);
        System.out.println("[Server] Player " + ch.getId() + " disconnected");

        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","DISCONNECT");
        msg.put("playerId", ch.getId());
        msg.put("name", ch.name);
        broadcast(msg);

        if (gameStarted && !gameOver) {
            if (ch.assignedRole == Role.CHOMPER) {
                endGame("CHASERS","CHOMPER_LEFT");
            }
        } else if (!gameStarted) {
            broadcastLobby();
        }
    }

    void broadcast(Map<String,Object> msg) {
        String json = NetworkUtils.toJson(msg);
        for (ClientHandler c : clients) c.sendRaw(json);
    }

    void broadcastLobby() {
        List<Object> playerList = new ArrayList<>();
        for (ClientHandler c : clients) {
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("id", c.getId());
            p.put("name", c.name);
            p.put("role", c.preferredRole != null ? c.preferredRole.name() : "NONE");
            p.put("ready", c.ready);
            playerList.add(p);
        }
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","LOBBY");
        msg.put("players", playerList);
        msg.put("hostId", clients.isEmpty() ? -1 : clients.get(0).getId());
        broadcast(msg);
    }

    private void broadcastState() {
        List<Object> playerList = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.player == null) continue;
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("id", c.getId());
            p.put("name", c.name);
            p.put("role", c.assignedRole != null ? c.assignedRole.name() : "CHASER");
            p.put("row", c.player.getRow());
            p.put("col", c.player.getCol());
            p.put("lives", c.player.getLives());
            p.put("score", c.player.getScore());
            p.put("powered", c.player.isPowered());
            p.put("direction", c.player.getDirection().name());
            playerList.add(p);
        }

        List<Object> pelletList = new ArrayList<>();
        for (model.Pellet pel : maze.getPellets()) {
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("row", pel.getRow());
            p.put("col", pel.getCol());
            p.put("power", pel.isPower());
            p.put("collected", pel.isCollected());
            pelletList.add(p);
        }

        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","STATE");
        msg.put("players", playerList);
        msg.put("pellets", pelletList);
        msg.put("timeLeft", (timeLeftTicks * GameConfig.TICK_MS) / 1000);
        broadcast(msg);
    }

    private Map<String,Object> buildError(String reason) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("type","ERROR");
        m.put("reason",reason);
        return m;
    }

    public static void main(String[] args) throws IOException {
        int port = GameConfig.SERVER_PORT;
        GameServer server = new GameServer(port);
        server.acceptLoop();
    }
}