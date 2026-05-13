package network;

import util.Direction;
import util.GameConfig;
import util.Role;

import java.io.*;
import java.net.Socket;

/**
 * Handles one client connection on the server side.
 * Reads one line at a time (TYPE|payload).
 * Runs in its own thread.
 */
public class ClientHandler implements Runnable {

    private final Socket     socket;
    private final int        playerId;
    private final GameServer server;
    private       String     playerName = "Player";
    private       Role       role       = null;
    private       boolean    ready      = false;
    private       PrintWriter out;

    public ClientHandler(Socket socket, int playerId, GameServer server) {
        this.socket   = socket;
        this.playerId = playerId;
        this.server   = server;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            // Send assigned ID immediately
            out.println("ID|" + playerId);

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            // Client disconnected
        } finally {
            server.onClientDisconnect(playerId);
            close();
        }
    }

    private void handleMessage(String line) {
        if (line.isEmpty()) return;
        int sep = line.indexOf('|');
        String type    = sep >= 0 ? line.substring(0, sep) : line;
        String payload = sep >= 0 ? line.substring(sep + 1) : "";

        switch (type) {
            case GameConfig.MSG_HELLO:
                playerName = payload.trim().isEmpty() ? "Player" + playerId : payload.trim();
                server.onClientReady(this);
                break;

            case GameConfig.MSG_READY:
                ready = true;
                break;

            case GameConfig.MSG_INPUT:
                try {
                    Direction dir = Direction.valueOf(payload.trim().toUpperCase());
                    server.onClientInput(playerId, dir);
                } catch (IllegalArgumentException ignored) {}
                break;

            case GameConfig.MSG_PING:
                send(GameConfig.MSG_PONG);
                break;

            default:
                break;
        }
    }

    public synchronized void send(String line) {
        if (out != null) out.println(line);
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    public int    getPlayerId()   { return playerId; }
    public String getPlayerName() { return playerName; }
    public Role   getRole()       { return role; }
    public boolean isReady()      { return ready; }
    public void   setRole(Role r) { this.role = r; }
}