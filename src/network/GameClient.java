package network;

import util.*;

import java.io.*;
import java.net.*;

/**
 * Network client for Milestone 2.
 *
 * - Connects to GameServer via TCP
 * - Sends player name (HELLO), then input (INPUT|DIRECTION) each key press
 * - Receives LOBBY, ROLE, COUNTDOWN, SNAPSHOT messages
 * - Notifies ClientEventListener on the Swing thread
 */
public class GameClient {

    public interface ClientEventListener {
        void onConnected(int assignedId);
        void onLobbyUpdate(LobbyState lobby);
        void onRoleAssigned(Role role, LobbyState lobby);
        void onSnapshot(GameSnapshot snapshot);
        void onError(String message);
        void onDisconnected();
    }

    private Socket       socket;
    private PrintWriter  out;
    private boolean      connected  = false;
    private int          myId       = -1;
    private Role         myRole     = null;
    private ClientEventListener listener;
    private String       serverHost;
    private int          serverPort;
    private String       playerName;

    public GameClient(String host, int port, String name) {
        this.serverHost = host;
        this.serverPort = port;
        this.playerName = name;
    }

    public void setListener(ClientEventListener l) { this.listener = l; }

    // ── Connect (call off EDT) ────────────────────────────────

    public void connect() throws IOException {
        socket    = new Socket();
        socket.connect(new InetSocketAddress(serverHost, serverPort), 5000);
        out       = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        connected = true;

        // Start read loop
        Thread t = new Thread(this::readLoop, "Client-Read");
        t.setDaemon(true);
        t.start();

        // Introduce ourselves
        send(GameConfig.MSG_HELLO + "|" + playerName);
    }

    private void readLoop() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            if (connected) notifyDisconnected();
        } finally {
            connected = false;
        }
    }

    private void handleMessage(String line) {
        if (line.isEmpty()) return;
        int sep    = line.indexOf('|');
        String type    = sep >= 0 ? line.substring(0, sep)    : line;
        String payload = sep >= 0 ? line.substring(sep + 1)   : "";

        switch (type) {
            case "ID":
                try { myId = Integer.parseInt(payload.trim()); }
                catch (NumberFormatException ignored) {}
                if (listener != null) invokeListener(() -> listener.onConnected(myId));
                break;

            case GameConfig.MSG_LOBBY_UPDATE: {
                LobbyState ls = LobbyState.fromJson(payload);
                if (listener != null) invokeListener(() -> listener.onLobbyUpdate(ls));
                break;
            }

            case GameConfig.MSG_ROLE_ASSIGN: {
                // payload: ROLE_NAME|{lobbyJson}
                int sp2 = payload.indexOf('|');
                String roleName   = sp2 >= 0 ? payload.substring(0, sp2) : payload;
                String lobbyJson  = sp2 >= 0 ? payload.substring(sp2 + 1) : "{}";
                try { myRole = Role.valueOf(roleName.trim()); } catch (Exception ignored) {}
                LobbyState ls = LobbyState.fromJson(lobbyJson);
                Role r = myRole;
                if (listener != null) invokeListener(() -> listener.onRoleAssigned(r, ls));
                break;
            }

            case GameConfig.MSG_SNAPSHOT: {
                GameSnapshot snap = GameSnapshot.fromJson(payload);
                if (listener != null) invokeListener(() -> listener.onSnapshot(snap));
                break;
            }

            case GameConfig.MSG_ERROR:
                if (listener != null) invokeListener(() -> listener.onError(payload));
                break;

            case GameConfig.MSG_DISCONNECT:
                notifyDisconnected();
                break;

            case GameConfig.MSG_PING:
                send(GameConfig.MSG_PONG);
                break;

            default:
                break;
        }
    }

    // ── Send input to server ─────────────────────────────────

    public void sendDirection(Direction dir) {
        if (connected) send(GameConfig.MSG_INPUT + "|" + dir.name());
    }

    public void sendReady() {
        if (connected) send(GameConfig.MSG_READY);
    }

    private synchronized void send(String line) {
        if (out != null) out.println(line);
    }

    // ── Lifecycle ────────────────────────────────────────────

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void notifyDisconnected() {
        connected = false;
        if (listener != null) invokeListener(() -> listener.onDisconnected());
    }

    private static void invokeListener(Runnable r) {
        javax.swing.SwingUtilities.invokeLater(r);
    }

    public boolean isConnected() { return connected; }
    public int     getMyId()     { return myId; }
    public Role    getMyRole()   { return myRole; }
    public String  getHost()     { return serverHost; }
    public int     getPort()     { return serverPort; }
}