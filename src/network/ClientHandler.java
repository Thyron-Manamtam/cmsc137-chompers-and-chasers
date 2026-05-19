package network;

import model.Player;
import util.Direction;
import util.Role;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private final int id;
    private final Socket socket;
    private final GameServer server;
    private PrintWriter out;

    // Lobby state
    public String name = "Player";
    public Role preferredRole = Role.CHASER;
    public boolean ready = false;
    public volatile boolean connected = true;

    // Game state
    public Role assignedRole = null;
    public Player player = null;
    public volatile Direction bufferedDirection = Direction.NONE;

    public ClientHandler(Socket socket, GameServer server, int id) {
        this.socket = socket;
        this.server = server;
        this.id     = id;
        this.name   = "Player" + id;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Map<String,Object> welcome = new LinkedHashMap<>();
            welcome.put("type","WELCOME");
            welcome.put("playerId", id);
            welcome.put("serverIp", NetworkUtils.getLocalIP());
            send(welcome);

            String line;
            while ((line = in.readLine()) != null) handleMessage(NetworkUtils.fromJson(line));
        } catch (IOException e) {
            // connection dropped
        } finally {
            connected = false;
            server.onDisconnect(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleMessage(Map<String,Object> msg) {
        String type = (String) msg.get("type");
        if (type == null) return;

        switch (type) {
            case "JOIN":
                if (msg.containsKey("name")) name = (String) msg.get("name");
                server.onJoin(this);
                break;
            case "ROLE":
                String roleStr = (String) msg.get("preferred");
                try { preferredRole = Role.valueOf(roleStr); } catch (Exception ignored) {}
                server.onRoleSelect(this);
                break;
            case "READY":
                ready = true;
                server.onReady(this);
                break;
            case "START":
                // Only host (first client) can start the game
                server.onStartGame(this);
                break;
            case "INPUT":
                String dir = (String) msg.get("direction");
                if (dir != null) server.onInput(this, dir);
                break;
            default:
                break;
        }
    }

    public void applyBufferedDirection() {
        if (player != null && bufferedDirection != null && bufferedDirection != Direction.NONE)
            player.setCurrentDirection(bufferedDirection);
    }

    public void send(Map<String,Object> msg) { sendRaw(NetworkUtils.toJson(msg)); }
    public void sendRaw(String json)         { if (out != null) out.println(json); }
    public int  getId()                      { return id; }
}