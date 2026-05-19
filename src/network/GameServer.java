package network;

import model.*;
import util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class GameServer {

    private final ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private volatile boolean gameStarted = false;
    private volatile boolean gameOver    = false;

    // Countdown state
    private volatile boolean countingDown = false;
    private int countdownTicks = 0;
    private static final int COUNTDOWN_TICKS = 12; // 3 seconds at 250 ms/tick

    private Maze maze;
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final Set<Integer> eliminatedChasers = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService gameLoop = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> loopHandle;

    private int timeLeftTicks;
    private int eatMultiplier = 1;

    public GameServer(int port) throws IOException {
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
                if (clients.size() >= GameConfig.MAX_PLAYERS) { sock.close(); continue; }

                ClientHandler ch = new ClientHandler(sock, this, nextId.getAndIncrement());
                clients.add(ch);
                new Thread(ch, "Client-" + ch.getId()).start();
                System.out.println("[Server] Player " + ch.getId() + " connected");
            } catch (IOException e) {
                if (!serverSocket.isClosed()) e.printStackTrace();
            }
        }
    }

    synchronized void onJoin(ClientHandler ch)       { broadcastLobby(); }
    synchronized void onRoleSelect(ClientHandler ch) { broadcastLobby(); }

    synchronized void onReady(ClientHandler ch) {
        broadcastLobby();
        checkAllReady();
    }

    private void checkAllReady() {
        if (gameStarted || countingDown) return;
        if (clients.size() < GameConfig.MIN_PLAYERS) return;
        for (ClientHandler c : clients) { if (!c.ready) return; }
        startCountdown();
    }

    private void startCountdown() {
        countingDown    = true;
        countdownTicks  = COUNTDOWN_TICKS;
        System.out.println("[Server] All players ready — starting 3-second countdown");

        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","COUNTDOWN_START");
        msg.put("seconds", 3);
        broadcast(msg);

        loopHandle = gameLoop.scheduleAtFixedRate(this::tickCountdown, GameConfig.TICK_MS, GameConfig.TICK_MS, TimeUnit.MILLISECONDS);
    }

    private synchronized void tickCountdown() {
        countdownTicks--;
        if (countdownTicks <= 0) {
            if (loopHandle != null) loopHandle.cancel(false);
            loopHandle     = null;
            countingDown   = false;
            startGameInternal();
        }
    }

    synchronized void onStartGame(ClientHandler requester) {
        if (gameStarted || countingDown) return;
        if (clients.isEmpty() || clients.get(0).getId() != requester.getId()) {
            requester.send(buildError("Only the host can force-start the game."));
            return;
        }
        if (clients.size() < GameConfig.MIN_PLAYERS) {
            requester.send(buildError("Need at least " + GameConfig.MIN_PLAYERS + " players to start."));
            return;
        }
        for (ClientHandler c : clients) {
            if (!c.ready) { requester.send(buildError("Not all players are ready yet.")); return; }
        }
        startGameInternal();
    }

    private synchronized void startGameInternal() {
        if (gameStarted) return;

        List<ClientHandler> wantChomper = new ArrayList<>();
        List<ClientHandler> wantChaser  = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.preferredRole == Role.CHOMPER) wantChomper.add(c);
            else wantChaser.add(c);
        }

        ClientHandler chomperClient;
        List<ClientHandler> chaserClients = new ArrayList<>();

        if (wantChomper.isEmpty()) {
            List<ClientHandler> shuffled = new ArrayList<>(clients);
            Collections.shuffle(shuffled);
            chomperClient = shuffled.get(0);
            for (int i = 1; i < shuffled.size(); i++) chaserClients.add(shuffled.get(i));
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

        maze = new Maze();
        players.clear();
        eliminatedChasers.clear();
        eatMultiplier   = 1;
        timeLeftTicks   = (GameConfig.GAME_DURATION_S * 1000) / GameConfig.TICK_MS;

        int[][] chaserSpawns = {{1,1},{1,13},{13,1},{13,13}};
        Player chomperPlayer = new Player(7, 7, Role.CHOMPER);
        players.put(chomperClient.getId(), chomperPlayer);
        chomperClient.player  = chomperPlayer;
        chomperClient.prevRow = 7; chomperClient.prevCol = 7;

        for (int i = 0; i < chaserClients.size(); i++) {
            ClientHandler cc = chaserClients.get(i);
            int[] sp = chaserSpawns[i % chaserSpawns.length];
            Player cp = new Player(sp[0], sp[1], Role.CHASER);
            players.put(cc.getId(), cp);
            cc.player  = cp;
            cc.prevRow = sp[0]; cc.prevCol = sp[1];
        }

        gameStarted = true;
        gameOver    = false;

        Map<String,Object> startMsg = new LinkedHashMap<>();
        startMsg.put("type","GAME_START");
        broadcast(startMsg);

        loopHandle = gameLoop.scheduleAtFixedRate(this::tick, 4000, GameConfig.TICK_MS, TimeUnit.MILLISECONDS);
        System.out.println("[Server] Game started with " + clients.size() + " players");
    }

    // ── Game tick ────────────────────────────────────────────────────────────

    private synchronized void tick() {
        if (gameOver) return;

        timeLeftTicks--;

        ClientHandler chomperCH = null;
        List<ClientHandler> chaserCHs = new ArrayList<>();

        for (ClientHandler ch : clients) {
            if (!ch.connected) continue;
            if (ch.assignedRole == Role.CHOMPER) chomperCH = ch;
            else if (!eliminatedChasers.contains(ch.getId())) chaserCHs.add(ch);
        }

        if (chomperCH == null) { endGame("CHASERS","CHOMPER_LEFT"); return; }

        // ── Snapshot positions BEFORE moving ────────────────────────────────
        chomperCH.snapshotPosition();
        for (ClientHandler ch : chaserCHs) ch.snapshotPosition();

        // ── Apply movement ───────────────────────────────────────────────────
        chomperCH.applyBufferedDirection();
        chomperCH.player.move(maze);

        boolean chomperPowered = chomperCH.player.isPowered();

        for (ClientHandler ch : chaserCHs) {
            if (!ch.connected) continue;
            ch.applyBufferedDirection();
            ch.player.move(maze);
        }

        // ── Collision detection (same-cell + cross-swap) ─────────────────────
        int chomperRow = chomperCH.player.getRow();
        int chomperCol = chomperCH.player.getCol();

        for (ClientHandler ch : chaserCHs) {
            if (!ch.connected || eliminatedChasers.contains(ch.getId())) continue;

            boolean sameCellNow = (ch.player.getRow() == chomperRow &&
                                   ch.player.getCol() == chomperCol);

            // Cross-swap: chaser was where chomper is now, and chomper was where chaser is now
            boolean crossSwap  = (ch.prevRow == chomperRow    && ch.prevCol == chomperCol &&
                                  chomperCH.prevRow == ch.player.getRow() &&
                                  chomperCH.prevCol == ch.player.getCol());

            if (!sameCellNow && !crossSwap) continue;

            // Collision confirmed – resolve it
            if (chomperCH.player.isPowered()) {
                eliminatedChasers.add(ch.getId());
                ch.player.loseLife();
                chomperCH.player.addScore(GameConfig.SCORE_EAT_BASE * eatMultiplier);
                eatMultiplier *= 2;

                Map<String,Object> elimMsg = new LinkedHashMap<>();
                elimMsg.put("type","CHASER_ELIMINATED");
                elimMsg.put("playerId", ch.getId());
                broadcast(elimMsg);

                if (allChasersEliminated()) { endGame("CHOMPERS","ALL_CHASERS_EATEN"); return; }
            } else {
                chomperCH.player.loseLife();
                if (!chomperCH.player.isAlive()) { endGame("CHASERS","NO_LIVES"); return; }
                for (ClientHandler cc : chaserCHs) {
                    if (!eliminatedChasers.contains(cc.getId())) cc.player.respawn();
                }
                chomperCH.player.respawn();      // respawn chomper too
                // Break so we only process one collision event per tick
                break;
            }
        }

        if (maze.countRemainingPellets() == 0) { endGame("CHOMPERS","ALL_PELLETS"); return; }
        if (timeLeftTicks <= 0)                { endGame("CHASERS","TIME_UP");      return; }

        broadcastState();
    }

    // ── Chat ─────────────────────────────────────────────────────────────────

    synchronized void onChat(ClientHandler sender, String text) {
        // Sanitise: strip leading/trailing whitespace, cap length
        String safe = text.trim();
        if (safe.length() > 120) safe = safe.substring(0, 120);
        if (safe.isEmpty()) return;

        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type",     "CHAT");
        msg.put("senderId", sender.getId());
        msg.put("sender",   sender.name);
        msg.put("text",     safe);
        broadcast(msg);
        System.out.println("[Chat] " + sender.name + ": " + safe);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean allChasersEliminated() {
        for (ClientHandler ch : clients) {
            if (ch.assignedRole == Role.CHASER && ch.connected && !eliminatedChasers.contains(ch.getId()))
                return false;
        }
        return true;
    }

    private void endGame(String winner, String reason) {
        gameOver = true;
        if (loopHandle != null) loopHandle.cancel(false);

        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type",   winner.equals("CHOMPERS") ? "WIN" : "GAME_OVER");
        msg.put("winner", winner);
        msg.put("reason", reason);
        broadcast(msg);
        System.out.println("[Server] Game over — winner: " + winner + " (" + reason + ")");
    }

    synchronized void onInput(ClientHandler ch, String direction) {
        if (ch.player == null) return;
        try { Direction d = Direction.valueOf(direction); ch.bufferedDirection = d; }
        catch (Exception ignored) {}
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
            if (ch.assignedRole == Role.CHOMPER) endGame("CHASERS","CHOMPER_LEFT");
        } else if (!gameStarted && !countingDown) {
            broadcastLobby();
        } else if (countingDown) {
            if (clients.size() < GameConfig.MIN_PLAYERS) {
                countingDown = false;
                if (loopHandle != null) { loopHandle.cancel(false); loopHandle = null; }
                Map<String,Object> cancel = new LinkedHashMap<>();
                cancel.put("type","COUNTDOWN_CANCEL");
                cancel.put("reason","A player disconnected");
                broadcast(cancel);
                broadcastLobby();
            }
        }
    }

    void broadcast(Map<String,Object> msg) {
        String json = NetworkUtils.toJson(msg);
        for (ClientHandler c : clients) c.sendRaw(json);
    }

    void broadcastLobby() {
        List<Object> playerList = new ArrayList<>();
        int hostId = clients.isEmpty() ? -1 : clients.get(0).getId();
        for (ClientHandler c : clients) {
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("id",    c.getId());
            p.put("name",  c.name);
            p.put("role",  c.preferredRole != null ? c.preferredRole.name() : "NONE");
            p.put("ready", c.ready);
            playerList.add(p);
        }
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","LOBBY");
        msg.put("players", playerList);
        msg.put("hostId",  hostId);
        broadcast(msg);
    }

    private void broadcastState() {
        List<Object> playerList = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.player == null) continue;
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("id",         c.getId());
            p.put("name",       c.name);
            p.put("role",       c.assignedRole != null ? c.assignedRole.name() : "CHASER");
            p.put("row",        c.player.getRow());
            p.put("col",        c.player.getCol());
            p.put("lives",      c.player.getLives());
            p.put("score",      c.player.getScore());
            p.put("powered",    c.player.isPowered());
            p.put("direction",  c.player.getDirection().name());
            p.put("eliminated", eliminatedChasers.contains(c.getId()));
            playerList.add(p);
        }
        List<Object> pelletList = new ArrayList<>();
        for (model.Pellet pel : maze.getPellets()) {
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("row",       pel.getRow());
            p.put("col",       pel.getCol());
            p.put("power",     pel.isPower());
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
        m.put("type","ERROR"); m.put("reason", reason);
        return m;
    }

    public static void main(String[] args) throws IOException {
        GameServer server = new GameServer(GameConfig.SERVER_PORT);
        server.acceptLoop();
    }
}