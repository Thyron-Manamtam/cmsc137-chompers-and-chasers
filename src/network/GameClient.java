package network;

import util.Direction;
import util.Role;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;

public class GameClient {

    private Socket socket;
    private PrintWriter out;
    private Thread readerThread;

    private final ClientGameState state = new ClientGameState();
    private Consumer<ClientGameState> onStateUpdate;
    private Consumer<String> onError;
    private Runnable onGameStart;
    private Runnable onGameOver;

    public void setOnStateUpdate(Consumer<ClientGameState> cb) { this.onStateUpdate = cb; }
    public void setOnError(Consumer<String> cb)                { this.onError = cb; }
    public void setOnGameStart(Runnable cb)                    { this.onGameStart = cb; }
    public void setOnGameOver(Runnable cb)                     { this.onGameOver = cb; }

    public ClientGameState getState() { return state; }

    public boolean connect(String host, int port, String playerName) {
        System.out.println("[Client] Attempting to connect to " + host + ":" + port);
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            System.out.println("[Client] Connected to " + host + ":" + port);

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
                        System.out.println("[Client] Received: " + line);
                        handleMessage(NetworkUtils.fromJson(line));
                    }
                } catch (IOException e) {
                    System.out.println("[Client] Reader thread ended: " + e.getMessage());
                    if (onError != null) onError.accept("Disconnected from server");
                }
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
                System.out.println("[Client] Got WELCOME, my ID = " + state.myId);
                break;

            case "LOBBY":
                parseLobbyPlayers(msg);
                state.hostId = toInt(msg.get("hostId"));
                if (onStateUpdate != null) onStateUpdate.accept(state);
                break;

            case "ROLE_ASSIGN":
                String roleStr = (String) msg.get("role");
                try { state.myRole = Role.valueOf(roleStr); } catch (Exception ignored) {}
                if (onStateUpdate != null) onStateUpdate.accept(state);
                break;

            case "GAME_START":
                System.out.println("[Client] Game starting!");
                if (onGameStart != null) onGameStart.run();
                break;

            case "STATE":
                parseGameState(msg);
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
            pi.connected = true;
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
                pi.id      = toInt(p.get("id"));
                pi.name    = (String)p.getOrDefault("name","?");
                pi.row     = toInt(p.get("row"));
                pi.col     = toInt(p.get("col"));
                pi.lives   = toInt(p.get("lives"));
                pi.score   = toInt(p.get("score"));
                pi.powered = Boolean.TRUE.equals(p.get("powered"));
                pi.direction = (String)p.getOrDefault("direction","NONE");
                pi.connected = true;
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

    private void sendMsg(Map<String,Object> msg) {
        if (out != null) {
            String json = NetworkUtils.toJson(msg);
            System.out.println("[Client] Sending: " + json);
            out.println(json);
        }
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private int toInt(Object v) {
        if (v instanceof Number) return ((Number)v).intValue();
        return 0;
    }
}