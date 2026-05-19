package network;

import util.Direction;
import util.Role;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GameClient {

    private Socket socket;
    private PrintWriter out;
    private Thread readerThread;

    private final ClientGameState state = new ClientGameState();
    private Consumer<ClientGameState>  onStateUpdate;
    private Consumer<String>           onError;
    private Runnable onGameStart;
    private Runnable onGameOver;
    private Runnable onCountdownStart;
    private Runnable onCountdownCancel;
    private Runnable onReturnToLobby;
    private Runnable onDisconnectedFromServer;
    private BiConsumer<String,String> onChat;
    // New: fired when server sends a super-pellet warning or relocation notice
    private Consumer<String> onSuperPelletNotice;

    public void setOnStateUpdate(Consumer<ClientGameState> cb)      { this.onStateUpdate         = cb; }
    public void setOnError(Consumer<String> cb)                     { this.onError               = cb; }
    public void setOnGameStart(Runnable cb)                         { this.onGameStart           = cb; }
    public void setOnGameOver(Runnable cb)                          { this.onGameOver            = cb; }
    public void setOnCountdownStart(Runnable cb)                    { this.onCountdownStart      = cb; }
    public void setOnCountdownCancel(Runnable cb)                   { this.onCountdownCancel     = cb; }
    public void setOnReturnToLobby(Runnable cb)                     { this.onReturnToLobby       = cb; }
    public void setOnDisconnectedFromServer(Runnable cb)            { this.onDisconnectedFromServer = cb; }
    public void setOnChat(BiConsumer<String,String> cb)             { this.onChat                = cb; }
    public void setOnSuperPelletNotice(Consumer<String> cb)         { this.onSuperPelletNotice   = cb; }

    public ClientGameState getState() { return state; }

    private volatile boolean gameHasStarted = false;

    public boolean connect(String host, int port, String playerName) {
        System.out.println("[Client] Connecting to " + host + ":" + port);
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            System.out.println("[Client] Connected");

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            state.myName = playerName;

            Map<String,Object> join = new LinkedHashMap<>();
            join.put("type","JOIN");
            join.put("name", playerName);
            sendMsg(join);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleMessage(NetworkUtils.fromJson(line));
                    }
                } catch (IOException e) {
                    System.out.println("[Client] Reader ended: " + e.getMessage());
                }
                if (onDisconnectedFromServer != null) onDisconnectedFromServer.run();
            }, "ClientReader");
            readerThread.setDaemon(true);
            readerThread.start();
            return true;

        } catch (SocketTimeoutException e) {
            if (onError != null) onError.accept("Connection timed out.\nCheck the IP and make sure the host has started hosting.");
            return false;
        } catch (ConnectException e) {
            if (onError != null) onError.accept("Connection refused.\nMake sure the host clicked 'Start Server & Host' first.");
            return false;
        } catch (IOException e) {
            if (onError != null) onError.accept("Cannot connect: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(Map<String,Object> msg) {
        String type = (String) msg.get("type");
        if (type == null) return;

        switch (type) {
            case "WELCOME":
                state.myId = toInt(msg.get("playerId"));
                break;

            case "LOBBY":
                parseLobbyPlayers(msg);
                state.hostId       = toInt(msg.get("hostId"));
                state.countingDown = false;
                if (gameHasStarted) {
                    gameHasStarted = false;
                    state.gameResult   = null;
                    state.resultReason = null;
                    if (onReturnToLobby != null) onReturnToLobby.run();
                } else {
                    if (onStateUpdate != null) onStateUpdate.accept(state);
                }
                break;

            case "ROLE_ASSIGN":
                String roleStr = (String) msg.get("role");
                try { state.myRole = Role.valueOf(roleStr); } catch (Exception ignored) {}
                if (onStateUpdate != null) onStateUpdate.accept(state);
                break;

            case "COUNTDOWN_START":
                state.countingDown     = true;
                state.countdownSeconds = toInt(msg.get("seconds"));
                if (onStateUpdate != null) onStateUpdate.accept(state);
                if (onCountdownStart != null) onCountdownStart.run();
                break;

            case "COUNTDOWN_CANCEL":
                state.countingDown     = false;
                state.countdownSeconds = 0;
                if (onStateUpdate != null) onStateUpdate.accept(state);
                if (onCountdownCancel != null) onCountdownCancel.run();
                break;

            case "GAME_START":
                gameHasStarted = true;
                state.countingDown = false;
                if (onGameStart != null) onGameStart.run();
                break;

            case "STATE":
                parseGameState(msg);
                if (onStateUpdate != null) onStateUpdate.accept(state);
                break;

            case "CHASER_ELIMINATED":
                int elimId = toInt(msg.get("playerId"));
                for (ClientGameState.PlayerInfo pi : state.players)
                    if (pi.id == elimId) { pi.eliminated = true; break; }
                if (onStateUpdate != null) onStateUpdate.accept(state);
                break;

            case "GAME_OVER":
            case "WIN":
                state.gameResult   = (String) msg.get("winner");
                state.resultReason = (String) msg.get("reason");
                if (onStateUpdate != null) onStateUpdate.accept(state);
                if (onGameOver != null) onGameOver.run();
                break;

            case "DISCONNECT":
                if (onStateUpdate != null) onStateUpdate.accept(state);
                break;

            case "CHAT":
                String sender = (String) msg.getOrDefault("sender","?");
                String text   = (String) msg.getOrDefault("text","");
                if (onChat != null) onChat.accept(sender, text);
                break;

            // ── New: super-pellet events ──────────────────────────────────────
            case "SUPER_PELLET_WARNING":
            case "SUPER_PELLET_RELOCATED":
                String notice = (String) msg.getOrDefault("message", type);
                if (onSuperPelletNotice != null) onSuperPelletNotice.accept(notice);
                break;

            case "ERROR":
                String reason = (String) msg.get("reason");
                System.out.println("[Client] Server error: " + reason);
                if (onError != null) onError.accept(reason);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void parseLobbyPlayers(Map<String,Object> msg) {
        state.players.clear();
        Object rawList = msg.get("players");
        if (!(rawList instanceof List)) return;
        for (Object item : (List<?>)rawList) {
            if (!(item instanceof Map)) continue;
            Map<String,Object> p = (Map<String,Object>)item;
            ClientGameState.PlayerInfo pi = new ClientGameState.PlayerInfo();
            pi.id    = toInt(p.get("id"));
            pi.name  = (String)p.getOrDefault("name","?");
            pi.ready = Boolean.TRUE.equals(p.get("ready"));
            pi.connected  = true;
            pi.eliminated = false;
            String r = (String)p.getOrDefault("role","CHASER");
            try { pi.role = Role.valueOf(r); } catch (Exception ignored) {}
            state.players.add(pi);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseGameState(Map<String,Object> msg) {
        state.timeLeft = toInt(msg.get("timeLeft"));
        state.players.clear();
        Object rawP = msg.get("players");
        if (rawP instanceof List) {
            for (Object item : (List<?>)rawP) {
                if (!(item instanceof Map)) continue;
                Map<String,Object> p = (Map<String,Object>)item;
                ClientGameState.PlayerInfo pi = new ClientGameState.PlayerInfo();
                pi.id        = toInt(p.get("id"));
                pi.name      = (String)p.getOrDefault("name","?");
                pi.row       = toInt(p.get("row")); pi.col = toInt(p.get("col"));
                pi.lives     = toInt(p.get("lives")); pi.score = toInt(p.get("score"));
                pi.powered   = Boolean.TRUE.equals(p.get("powered"));
                pi.direction = (String)p.getOrDefault("direction","NONE");
                pi.connected = true;
                pi.eliminated = Boolean.TRUE.equals(p.get("eliminated"));
                String r = (String)p.getOrDefault("role","CHASER");
                try { pi.role = Role.valueOf(r); } catch (Exception ignored) {}
                state.players.add(pi);
            }
        }
        state.pellets.clear();
        Object rawPel = msg.get("pellets");
        if (rawPel instanceof List) {
            for (Object item : (List<?>)rawPel) {
                if (!(item instanceof Map)) continue;
                Map<String,Object> p = (Map<String,Object>)item;
                ClientGameState.PelletInfo pi = new ClientGameState.PelletInfo();
                pi.row = toInt(p.get("row")); pi.col = toInt(p.get("col"));
                pi.power = Boolean.TRUE.equals(p.get("power"));
                pi.collected = Boolean.TRUE.equals(p.get("collected"));
                state.pellets.add(pi);
            }
        }
    }

    // ── Sending ───────────────────────────────────────────────────────────────

    public void sendInput(Direction d) {
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","INPUT"); msg.put("direction", d.name());
        sendMsg(msg);
    }

    public void sendRole(Role role) {
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","ROLE"); msg.put("preferred", role.name());
        sendMsg(msg);
    }

    public void sendReady() {
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","READY");
        sendMsg(msg);
    }

    public void sendStart() {
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","START");
        sendMsg(msg);
    }

    public void sendChat(String text) {
        if (text == null || text.isBlank()) return;
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","CHAT"); msg.put("text", text.trim());
        sendMsg(msg);
    }

    public void sendPlayAgain() {
        Map<String,Object> msg = new LinkedHashMap<>();
        msg.put("type","PLAY_AGAIN");
        sendMsg(msg);
    }

    private void sendMsg(Map<String,Object> msg) {
        if (out != null) out.println(NetworkUtils.toJson(msg));
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private int toInt(Object v) {
        if (v instanceof Number) return ((Number)v).intValue();
        return 0;
    }
}