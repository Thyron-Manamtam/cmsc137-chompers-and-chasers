package network;

import model.*;
import util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Authoritative game server for Milestone 2.
 *
 * - Accepts up to 4 TCP client connections on GameConfig.SERVER_PORT
 * - Waits in LOBBY until 4 players have joined (or host starts with fewer)
 * - Assigns 1 Chomper + 3 Chasers
 * - Runs the authoritative game loop in its own thread
 * - Broadcasts GameSnapshot JSON to all clients every tick
 * - Reads Direction input from each client
 */
public class GameServer {

    // ── Lobby info ───────────────────────────────────────────
    private final List<ClientHandler> clients  = new CopyOnWriteArrayList<>();
    private final AtomicInteger       nextId   = new AtomicInteger(0);
    private       boolean             gameStarted = false;
    private       boolean             running     = false;
    private       ServerSocket        serverSocket;

    // ── Game model (lives on server) ─────────────────────────
    private Maze         maze;
    private Player       chomper;
    private List<Chaser> chasers; // may be AI or human-driven
    private GameState    state;
    private int          deathAnimTicks;
    private int          eatMultiplier;
    private int          countdownVal;

    // Listener so the host's UI can react
    private ServerEventListener listener;

    public interface ServerEventListener {
        void onPlayerJoined(LobbyState lobby);
        void onPlayerLeft(int playerId);
        void onGameStarting();
    }

    public void setListener(ServerEventListener l) { this.listener = l; }

    // ── Start listening ──────────────────────────────────────

    public void startListening(int port) throws IOException {
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
        running      = true;
        Thread acceptThread = new Thread(this::acceptLoop, "Server-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println("[Server] Listening on port " + port);
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket sock = serverSocket.accept();
                if (gameStarted || clients.size() >= GameConfig.MAX_PLAYERS) {
                    // Reject late joiners
                    PrintWriter pw = new PrintWriter(sock.getOutputStream(), true);
                    String reason = gameStarted ? "Game already started" : "Lobby is full";
                    pw.println(GameConfig.MSG_ERROR + "|" + reason);
                    sock.close();
                    continue;
                }
                int id = nextId.getAndIncrement();
                ClientHandler handler = new ClientHandler(sock, id, this);
                clients.add(handler);
                Thread t = new Thread(handler, "Client-" + id);
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) System.err.println("[Server] Accept error: " + e.getMessage());
            }
        }
    }

    // ── Called by ClientHandler after HELLO ─────────────────

    synchronized void onClientReady(ClientHandler handler) {
        broadcastLobby();
        if (listener != null) listener.onPlayerJoined(buildLobbyState());
    }

    synchronized void onClientInput(int clientId, Direction dir) {
        if (!gameStarted || state != GameState.PLAYING) return;
        // Find who this client controls
        if (chomper != null && chomper.getPlayerId() == clientId) {
            chomper.setCurrentDirection(dir);
        } else {
            for (Chaser c : chasers) {
                if (c.isHuman() && c.getPlayerId() == clientId) {
                    applyHumanChaserDir(c, dir);
                    // Check collision immediately after movement
                    if (!c.isEaten() && c.catches(chomper)) {
                        if (chomper.isPowered()) {
                            c.eat();
                            chomper.addScore(GameConfig.SCORE_EAT_BASE * eatMultiplier);
                            eatMultiplier *= 2;
                        } else {
                            handleDeath();
                        }
                    }
                    break;
                }
            }
        }
    }

    private void applyHumanChaserDir(Chaser c, Direction dir) {
        if (c.isEaten()) return;
        int nr = c.getRow() + dir.getDRow();
        int nc = c.getCol() + dir.getDCol();
        if (!maze.isWall(nr, nc)) {
            c.setRow(nr);
            c.setCol(nc);
        }
    }

    synchronized void onClientDisconnect(int clientId) {
        clients.removeIf(h -> h.getPlayerId() == clientId);
        broadcastLobby();
        if (listener != null) listener.onPlayerLeft(clientId);
        if (gameStarted && clients.isEmpty()) {
            stopServer();
        }
    }

    // ── Host starts the game ─────────────────────────────────

    public synchronized void startGame() {
        if (gameStarted || clients.size() < GameConfig.MIN_PLAYERS) return;
        gameStarted = true;
        assignRoles();
        broadcastRoles();
        if (listener != null) listener.onGameStarting();
        // Countdown then game loop
        Thread gameThread = new Thread(this::countdownThenPlay, "Server-GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void assignRoles() {
        // Randomly select one client as Chomper; rest = Chasers
        maze         = new Maze();
        eatMultiplier = 1;
        chasers       = new ArrayList<>();

        int[][] spawnPoints = {{1,1},{1,13},{13,1},{13,13}};
        
        // Randomly pick which client becomes the Chomper
        int chomperIndex = new java.util.Random().nextInt(clients.size());

        for (int i = 0; i < clients.size(); i++) {
            ClientHandler h = clients.get(i);
            int[] sp = spawnPoints[i % spawnPoints.length];
            if (i == chomperIndex) {
                // Chomper
                chomper = new Player(7, 7, Role.CHOMPER);
                chomper.setPlayerName(h.getPlayerName());
                chomper.setPlayerId(h.getPlayerId());
                h.setRole(Role.CHOMPER);
            } else {
                // Human Chaser
                Chaser c = new Chaser(sp[0], sp[1], 1);
                c.setPlayerName(h.getPlayerName());
                c.setPlayerId(h.getPlayerId());
                h.setRole(Role.CHASER);
                chasers.add(c);
            }
        }

        // Fill remaining slots with AI chasers if fewer than 4 humans
        int aiCount = 0;
        int[] delays = {1, 2, 2};
        for (int i = clients.size(); i < GameConfig.MAX_PLAYERS; i++) {
            int[] sp = spawnPoints[i % spawnPoints.length];
            Chaser ai = new Chaser(sp[0], sp[1], delays[aiCount % delays.length]);
            chasers.add(ai);
            aiCount++;
        }
    }

    private void broadcastRoles() {
        for (ClientHandler h : clients) {
            String roleName = h.getRole() == Role.CHOMPER ? "CHOMPER" : "CHASER";
            h.send(GameConfig.MSG_ROLE_ASSIGN + "|" + roleName + "|" + buildLobbyState().toJson());
        }
    }

    private void countdownThenPlay() {
        state = GameState.COUNTDOWN;
        for (int cd = GameConfig.COUNTDOWN_TICKS; cd >= 1; cd--) {
            countdownVal = cd;
            broadcastSnapshot();
            sleep(1000);
        }
        state         = GameState.PLAYING;
        deathAnimTicks = 0;
        gameLoop();
    }

    private void gameLoop() {
        while (running && gameStarted) {
            tickPlaying();
            broadcastSnapshot();
            sleep(GameConfig.TICK_MS);
        }
    }

    private synchronized void tickPlaying() {
        if (state != GameState.PLAYING) {
            if (state == GameState.DEAD) {
                if (--deathAnimTicks <= 0) state = GameState.PLAYING;
            }
            return;
        }

        // Move chomper
        chomper.move(maze);

        // Propagate power
        if (chomper.isPowered()) {
            for (Chaser c : chasers) { if (!c.isEaten()) c.frighten(); }
        } else {
            for (Chaser c : chasers) c.tickFrightened();
        }

        // Move AI chasers; human chasers already moved via input
        for (Chaser c : chasers) {
            if (!c.isHuman() && !c.isEaten()) c.moveToward(chomper, maze);
        }

        // Collision detection — check all chasers first
        List<Chaser> chasersToCatch = new ArrayList<>();
        for (Chaser c : chasers) {
            if (!c.isEaten() && c.catches(chomper)) {
                chasersToCatch.add(c);
            }
        }

        // Handle collisions
        if (!chasersToCatch.isEmpty()) {
            if (chomper.isPowered()) {
                // Eat all caught chasers in powered mode
                for (Chaser c : chasersToCatch) {
                    c.eat();
                    chomper.addScore(GameConfig.SCORE_EAT_BASE * eatMultiplier);
                    eatMultiplier *= 2;
                }
            } else {
                // Get caught by any chaser in normal mode
                handleDeath();
                return;
            }
        }

        // Respawn eaten chasers if power wears off
        if (!chomper.isPowered()) {
            for (Chaser c : chasers) {
                if (c.isEaten()) {
                    c.respawn();
                }
            }
        }

        // Win condition
        if (maze.countRemainingPellets() == 0) {
            state = GameState.WIN;
        }
    }

    private void handleDeath() {
        chomper.loseLife();
        if (!chomper.isAlive()) {
            state = GameState.GAME_OVER;
        } else {
            state          = GameState.DEAD;
            deathAnimTicks = GameConfig.DEATH_TICKS;
            for (Chaser c : chasers) c.respawn();
            eatMultiplier  = 1;
        }
    }

    // ── Broadcast helpers ────────────────────────────────────

    private void broadcastSnapshot() {
        if (chomper == null) return;
        GameSnapshot snap = buildSnapshot();
        String line = GameConfig.MSG_SNAPSHOT + "|" + snap.toJson();
        for (ClientHandler h : clients) h.send(line);
    }

    private GameSnapshot buildSnapshot() {
        GameSnapshot s = new GameSnapshot();
        s.state           = state.name();
        s.deathTicks      = deathAnimTicks;
        s.countdown       = countdownVal;
        s.pelletsCollected= maze.getCollectedMask();

        s.chomperPlayerId = chomper.getPlayerId();
        s.chomperName     = chomper.getPlayerName();
        s.chomperRow      = chomper.getRow();
        s.chomperCol      = chomper.getCol();
        s.chomperScore    = chomper.getScore();
        s.chomperLives    = chomper.getLives();
        s.chomperPowered  = chomper.isPowered();
        s.chomperPowerTicks=chomper.getPowerTicks();
        s.chomperMouthFrame=chomper.getMouthFrame();
        s.chomperDir      = chomper.getDirection().name();

        s.chasers = new GameSnapshot.ChaserData[chasers.size()];
        for (int i = 0; i < chasers.size(); i++) {
            Chaser c = chasers.get(i);
            s.chasers[i] = new GameSnapshot.ChaserData(
                c.getPlayerId(),
                c.isHuman() ? c.getPlayerName() : "AI-" + i,
                c.getRow(), c.getCol(),
                c.getMode().name(),
                c.getFrightenedTicks(),
                c.isHuman()
            );
        }

        s.winnerName = (state == GameState.WIN) ? chomper.getPlayerName()
                     : (state == GameState.GAME_OVER) ? "Chasers" : "";
        return s;
    }

    private void broadcastLobby() {
        LobbyState ls = buildLobbyState();
        String line = GameConfig.MSG_LOBBY_UPDATE + "|" + ls.toJson();
        for (ClientHandler h : clients) h.send(line);
    }

    LobbyState buildLobbyState() {
        LobbyState ls = new LobbyState();
        ls.hostId = clients.isEmpty() ? -1 : clients.get(0).getPlayerId();
        for (ClientHandler h : clients) {
            String roleName = h.getRole() == null ? "TBD"
                            : h.getRole() == Role.CHOMPER ? "CHOMPER" : "CHASER";
            ls.players.add(new LobbyState.PlayerInfo(h.getPlayerId(), h.getPlayerName(), roleName, h.isReady()));
        }
        return ls;
    }

    // ── Lifecycle ────────────────────────────────────────────

    public void stopServer() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        for (ClientHandler h : clients) h.close();
    }

    public boolean isGameStarted() { return gameStarted; }
    public int     getPlayerCount(){ return clients.size(); }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ── Entry point (standalone server mode) ─────────────────

    public static void main(String[] args) {
        GameServer server = new GameServer();
        try {
            server.startListening(GameConfig.SERVER_PORT);
            System.out.println("Press Enter to stop.");
            new java.util.Scanner(System.in).nextLine();
            server.stopServer();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}