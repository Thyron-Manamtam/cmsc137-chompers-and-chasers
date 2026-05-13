package view;

import controller.GameController;
import network.*;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * Unified game panel for all modes: single-player, host, and client.
 *
 * Screen flow (multiplayer):
 *   START → (mode select) → JOINING → LOBBY → ROLE_REVEAL → COUNTDOWN → PLAYING → WIN/GAME_OVER
 *
 * In single-player mode the flow is identical to M1.
 */
public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private static final int TILE = 40;
    private static final int W    = 15 * TILE;  // 600
    private static final int H    = 15 * TILE;  // 600

    // ── Mode ─────────────────────────────────────────────────
    private AppMode appMode = null; // null = on title screen (not yet chosen)

    // ── Single-player ─────────────────────────────────────────
    private GameController singleController;

    // ── Multiplayer ───────────────────────────────────────────
    private GameClient  client;
    private GameServer  server; // only when hosting
    private GameState   mpState  = GameState.START;
    private GameSnapshot lastSnap = null;
    private LobbyState   lastLobby= null;
    private Role         myRole   = null;
    private String       myName   = "Player";
    private String       errorMsg = "";
    private String       serverAddr= "";
    private boolean      isHost   = false;
    private int          countdown= 0;

    // ── Input dialogs state ───────────────────────────────────
    // We use lightweight in-panel text fields instead of JOptionPane
    private InputDialog activeDialog = null;

    private final GameRenderer renderer;

    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        renderer = new GameRenderer();
        // Start on the multiplayer title (mode selection)
        mpState  = GameState.START;
    }

    // ── Paint ─────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (appMode == AppMode.SINGLE_PLAYER && singleController != null) {
            renderer.render(g2, singleController, getWidth(), getHeight());
        } else {
            renderer.renderMultiplayer(g2, getWidth(), getHeight(),
                    mpState, lastSnap, lastLobby, myName, myRole,
                    errorMsg, serverAddr, countdown, isHost);
        }

        // Draw input dialog on top if active
        if (activeDialog != null) {
            activeDialog.draw(g2, getWidth(), getHeight());
        }
    }

    // ── Mode switching ────────────────────────────────────────

    private void startSinglePlayer() {
        appMode          = AppMode.SINGLE_PLAYER;
        singleController = new GameController(this);
        singleController.startGame();
        repaint();
    }

    private void showMultiplayerMenu() {
        mpState = GameState.MP_MENU;
        repaint();
    }

    private void showHostDialog() {
        activeDialog = new InputDialog("HOST A GAME",
            new String[]{"Your name:", "Port:"},
            new String[]{"Player1", String.valueOf(GameConfig.SERVER_PORT)},
            result -> {
                activeDialog = null;
                if (result == null) { repaint(); return; }
                myName = result[0].trim().isEmpty() ? "Player1" : result[0].trim();
                int port = GameConfig.SERVER_PORT;
                try { port = Integer.parseInt(result[1].trim()); } catch (Exception ignored) {}
                doHostGame(myName, port);
            });
        repaint();
    }

    private void showJoinDialog() {
        activeDialog = new InputDialog("JOIN A GAME",
            new String[]{"Server address:", "Port:", "Your name:"},
            new String[]{GameConfig.DEFAULT_HOST, String.valueOf(GameConfig.SERVER_PORT), "Player1"},
            result -> {
                activeDialog = null;
                if (result == null) { repaint(); return; }
                String host = result[0].trim().isEmpty() ? GameConfig.DEFAULT_HOST : result[0].trim();
                int    port  = GameConfig.SERVER_PORT;
                try { port = Integer.parseInt(result[1].trim()); } catch (Exception ignored) {}
                myName = result[2].trim().isEmpty() ? "Player1" : result[2].trim();
                doJoinGame(host, port, myName);
            });
        repaint();
    }

    private void doHostGame(String name, int port) {
        appMode  = AppMode.HOST;
        isHost   = true;
        serverAddr = "localhost:" + port;
        mpState  = GameState.JOINING;
        repaint();

        // Start server in background
        server = new GameServer();
        server.setListener(new GameServer.ServerEventListener() {
            @Override public void onPlayerJoined(LobbyState lobby) {
                SwingUtilities.invokeLater(() -> { lastLobby = lobby; mpState = GameState.LOBBY; repaint(); });
            }
            @Override public void onPlayerLeft(int id) {
                SwingUtilities.invokeLater(() -> repaint());
            }
            @Override public void onGameStarting() {
                SwingUtilities.invokeLater(() -> mpState = GameState.ROLE_REVEAL);
            }
        });

        new Thread(() -> {
            try {
                server.startListening(port);
                // Now connect our own client to it
                SwingUtilities.invokeLater(() -> doJoinAsClient("localhost", port, name));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> showError("Failed to start server: " + e.getMessage(), ""));
            }
        }, "StartServer").start();
    }

    private void doJoinAsClient(String host, int port, String name) {
        doJoinGame(host, port, name);
    }

    private void doJoinGame(String host, int port, String name) {
        appMode    = appMode == AppMode.HOST ? AppMode.HOST : AppMode.CLIENT;
        myName     = name;
        serverAddr = host + ":" + port;
        mpState    = GameState.JOINING;
        repaint();

        client = new GameClient(host, port, name);
        client.setListener(new GameClient.ClientEventListener() {
            @Override public void onConnected(int id) {
                SwingUtilities.invokeLater(() -> { mpState = GameState.LOBBY; repaint(); });
            }
            @Override public void onLobbyUpdate(LobbyState lobby) {
                SwingUtilities.invokeLater(() -> { lastLobby = lobby; mpState = GameState.LOBBY; repaint(); });
            }
            @Override public void onRoleAssigned(Role role, LobbyState lobby) {
                SwingUtilities.invokeLater(() -> {
                    myRole    = role;
                    lastLobby = lobby;
                    mpState   = GameState.ROLE_REVEAL;
                    repaint();
                });
            }
            @Override public void onSnapshot(GameSnapshot snap) {
                SwingUtilities.invokeLater(() -> {
                    lastSnap = snap;
                    try { mpState = GameState.valueOf(snap.state); }
                    catch (Exception ignored) { mpState = GameState.PLAYING; }
                    countdown = snap.countdown;
                    repaint();
                });
            }
            @Override public void onError(String msg) {
                SwingUtilities.invokeLater(() -> showError(msg, serverAddr));
            }
            @Override public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    if (mpState == GameState.PLAYING || mpState == GameState.DEAD) {
                        mpState = GameState.DISCONNECTED;
                    } else {
                        showError("Disconnected from server", serverAddr);
                    }
                    repaint();
                });
            }
        });

        new Thread(() -> {
            try {
                client.connect();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> showError("Cannot connect to " + host + ":" + port + " — " + e.getMessage(), host+":"+port));
            }
        }, "ClientConnect").start();
    }

    private void showError(String msg, String addr) {
        errorMsg   = msg;
        serverAddr = addr;
        mpState    = GameState.ERROR;
        repaint();
    }

    // ── Key handling ──────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        // Dialog intercepts first
        if (activeDialog != null) {
            activeDialog.keyPressed(e, this::repaint);
            return;
        }

        int key = e.getKeyCode();

        // Single-player mode
        if (appMode == AppMode.SINGLE_PLAYER) {
            switch (key) {
                case KeyEvent.VK_UP:    singleController.setPlayerDirection(Direction.UP);    break;
                case KeyEvent.VK_DOWN:  singleController.setPlayerDirection(Direction.DOWN);  break;
                case KeyEvent.VK_LEFT:  singleController.setPlayerDirection(Direction.LEFT);  break;
                case KeyEvent.VK_RIGHT: singleController.setPlayerDirection(Direction.RIGHT); break;
                case KeyEvent.VK_P:     singleController.togglePause();                       break;
                case KeyEvent.VK_R:     singleController.restartGame();                       break;
                case KeyEvent.VK_ESCAPE:
                    singleController = null; appMode = null; mpState = GameState.START; repaint(); break;
                case KeyEvent.VK_ENTER:
                    if (singleController.getState()==GameState.START) singleController.startGame(); break;
            }
            return;
        }

        // Multiplayer arrow key input
        if (client != null && (mpState == GameState.PLAYING || mpState == GameState.DEAD)) {
            Direction dir = null;
            switch (key) {
                case KeyEvent.VK_UP:    dir = Direction.UP;    break;
                case KeyEvent.VK_DOWN:  dir = Direction.DOWN;  break;
                case KeyEvent.VK_LEFT:  dir = Direction.LEFT;  break;
                case KeyEvent.VK_RIGHT: dir = Direction.RIGHT; break;
            }
            if (dir != null) { client.sendDirection(dir); return; }
        }

        // ESC → back to title
        if (key == KeyEvent.VK_ESCAPE) {
            if (mpState == GameState.MP_MENU) {
                mpState = GameState.START;
            } else {
                disconnect();
                appMode = null; mpState = GameState.START; 
            }
            repaint();
            return;
        }

        // R → restart (only single-player; multiplayer restart needs host action)
        if (key == KeyEvent.VK_R && appMode == AppMode.SINGLE_PLAYER) {
            singleController.restartGame();
        }

        // ENTER on role reveal → send ready
        if (key == KeyEvent.VK_ENTER && mpState == GameState.ROLE_REVEAL) {
            if (client != null) client.sendReady();
        }

        // ENTER on lobby → host starts game
        if (key == KeyEvent.VK_ENTER && mpState == GameState.LOBBY && isHost && server != null) {
            server.startGame();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // ── Mouse handling (button clicks on title screen) ────────

    @Override
    public void mouseClicked(MouseEvent e) {
        if (activeDialog != null) {
            activeDialog.mouseClicked(e, this);
            return;
        }
        int mx = e.getX(), my = e.getY();
        int w = getWidth(), h = getHeight();

        if (mpState == GameState.MP_MENU) {
            // Multiplayer menu buttons: Host Game, Join Game
            int bw=260, bh=50, bx=w/2-bw/2;
            int hostY = h/2-30;
            int joinY = h/2+40;
            if (mx>=bx && mx<bx+bw) {
                if (my>=hostY && my<hostY+bh)  { showHostDialog();    return; }
                if (my>=joinY && my<joinY+bh)  { showJoinDialog();    return; }
            }
        }

        if (mpState == GameState.START && appMode == null) {
            // Main menu buttons: Single Player, Multiplayer
            int bw=260, bh=50, bx=w/2-bw/2;
            int spY   = h/2-30;
            int mpY   = h/2+40;
            if (mx>=bx && mx<bx+bw) {
                if (my>=spY   && my<spY+bh)   { startSinglePlayer(); return; }
                if (my>=mpY && my<mpY+bh)  { showMultiplayerMenu();    return; }
            }
        }

        // Lobby: host clicks Start
        if (mpState == GameState.LOBBY && isHost && server != null) {
            int slotH=36*4+44+36+54, btnW=200, btnH=34;
            int btnX=w/2-btnW/2, btnY=slotH+8; // approximate
            if (mx>=btnX && mx<=btnX+btnW && my>=btnY && my<=btnY+btnH) {
                server.startGame();
            }
        }

        // Role reveal: "READY" button
        if (mpState == GameState.ROLE_REVEAL && client != null) {
            int bw=200,bh=36,bx=w/2-bw/2,by=h-60;
            if (mx>=bx&&mx<=bx+bw&&my>=by&&my<=by+bh) {
                client.sendReady();
            }
        }

        // Error: "Try again"
        if (mpState == GameState.ERROR) {
            int btnW=200,btnH=34,btnX=w/2-btnW/2,btnY=h/2+30;
            if (mx>=btnX&&mx<=btnX+btnW&&my>=btnY&&my<=btnY+btnH) {
                disconnect(); mpState=GameState.START; appMode=null; repaint();
            }
        }

        // Disconnected: Reconnect / Quit
        if (mpState == GameState.DISCONNECTED) {
            int bw=300,bh=160,bx=w/2-bw/2,by=h/2-bh/2;
            int btn1X=w/2-110-6, btn1Y=by+bh-48;
            int btn2X=w/2+6,     btn2Y=by+bh-48;
            if (mx>=btn2X&&mx<=btn2X+110&&my>=btn2Y&&my<=btn2Y+30) {
                disconnect(); mpState=GameState.START; appMode=null; repaint();
            }
            if (mx>=btn1X&&mx<=btn1X+110&&my>=btn1Y&&my<=btn1Y+30) {
                // Reconnect: rejoin with same info
                String[] parts = serverAddr.split(":");
                if (parts.length==2) {
                    try { doJoinGame(parts[0], Integer.parseInt(parts[1]), myName); }
                    catch (Exception ex) { showError("Reconnect failed", serverAddr); }
                }
            }
        }
    }

    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}

    private void disconnect() {
        if (client != null) { client.disconnect(); client = null; }
        if (server != null && !isHost) { /* client-only */ }
        // Don't stop the server if we're host — other players still connected
        server = null; isHost = false;
        lastSnap = null; lastLobby = null; myRole = null;
    }

    // ── Inner class: lightweight in-panel input dialog ────────

    /**
     * A simple overlay dialog drawn directly on the panel canvas.
     * Supports multiple text fields with keyboard navigation.
     */
    static class InputDialog {
        private final String   title;
        private final String[] labels;
        private final String[] values;
        private final java.util.function.Consumer<String[]> callback;
        private int focusedField = 0;

        InputDialog(String title, String[] labels, String[] defaults,
                    java.util.function.Consumer<String[]> callback) {
            this.title    = title;
            this.labels   = labels;
            this.values   = defaults.clone();
            this.callback = callback;
        }

        void draw(Graphics2D g2, int w, int h) {
            // Dim background
            g2.setColor(new Color(0,0,0,200));
            g2.fillRect(0, 0, w, h);

            int dw=320, fieldH=32, padding=20;
            int dh = 60 + labels.length*(fieldH+28) + 60;
            int dx = w/2-dw/2, dy = h/2-dh/2;

            // Dialog box
            g2.setColor(new Color(10,10,26));
            g2.fillRoundRect(dx,dy,dw,dh,10,10);
            g2.setColor(new Color(55,88,221));
            g2.drawRoundRect(dx,dy,dw,dh,10,10);

            // Title
            g2.setFont(new Font("Courier New",Font.BOLD,18));
            g2.setColor(new Color(255,215,0));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, dx+(dw-fm.stringWidth(title))/2, dy+32);

            // Fields
            int fy = dy+55;
            for (int i=0; i<labels.length; i++) {
                g2.setFont(new Font("Courier New",Font.PLAIN,11));
                g2.setColor(new Color(136,136,136));
                g2.drawString(labels[i].toUpperCase(), dx+padding, fy+14);

                fy += 18;
                boolean focused = (i==focusedField);
                g2.setColor(focused ? new Color(13,13,50) : new Color(13,13,34));
                g2.fillRoundRect(dx+padding, fy, dw-padding*2, fieldH, 4,4);
                g2.setColor(focused ? new Color(55,138,221) : new Color(51,51,51));
                g2.drawRoundRect(dx+padding, fy, dw-padding*2, fieldH, 4,4);
                g2.setFont(new Font("Courier New",Font.PLAIN,13));
                g2.setColor(Color.WHITE);
                g2.drawString(values[i], dx+padding+8, fy+21);

                // Cursor blink
                if (focused && (System.currentTimeMillis()/500)%2==0) {
                    fm = g2.getFontMetrics();
                    int cx2 = dx+padding+8+fm.stringWidth(values[i]);
                    g2.drawLine(cx2, fy+6, cx2, fy+fieldH-6);
                }
                fy += fieldH+8;
            }

            // Buttons
            int btnY = fy+8;
            int bw2=120, bh2=32;
            // Cancel
            g2.setColor(new Color(40,40,60));
            g2.fillRoundRect(dx+padding, btnY, bw2, bh2, 4,4);
            g2.setColor(new Color(80,80,100));
            g2.drawRoundRect(dx+padding, btnY, bw2, bh2, 4,4);
            g2.setFont(new Font("Courier New",Font.BOLD,13));
            g2.setColor(new Color(160,160,160));
            fm = g2.getFontMetrics();
            g2.drawString("Cancel", dx+padding+(bw2-fm.stringWidth("Cancel"))/2, btnY+21);
            // Connect/OK
            g2.setColor(new Color(255,215,0));
            g2.fillRoundRect(dx+dw-padding-bw2, btnY, bw2, bh2, 4,4);
            g2.setFont(new Font("Courier New",Font.BOLD,13));
            g2.setColor(Color.BLACK);
            fm = g2.getFontMetrics();
            String okLabel = title.startsWith("HOST") ? "Start" : title.startsWith("JOIN") ? "Connect" : "OK";
            g2.drawString(okLabel, dx+dw-padding-bw2+(bw2-fm.stringWidth(okLabel))/2, btnY+21);
        }

        void keyPressed(KeyEvent e, Runnable repaint) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_ESCAPE) { callback.accept(null); repaint.run(); return; }
            if (key == KeyEvent.VK_ENTER)  { callback.accept(values); repaint.run(); return; }
            if (key == KeyEvent.VK_TAB) {
                focusedField = (focusedField+1) % labels.length;
                repaint.run(); return;
            }
            if (key == KeyEvent.VK_BACK_SPACE) {
                String v = values[focusedField];
                if (!v.isEmpty()) values[focusedField] = v.substring(0, v.length()-1);
                repaint.run(); return;
            }
            char c = e.getKeyChar();
            if (c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c)) {
                values[focusedField] += c;
                repaint.run();
            }
        }

        void mouseClicked(MouseEvent e, GamePanel panel) {
            // Check field clicks and button clicks
            int w = panel.getWidth(), h = panel.getHeight();
            int dw=320, fieldH=32, padding=20;
            int dh = 60 + labels.length*(fieldH+28) + 60;
            int dx = w/2-dw/2, dy = h/2-dh/2;

            int fy = dy+55;
            for (int i=0; i<labels.length; i++) {
                fy += 18;
                if (e.getX()>=dx+padding && e.getX()<=dx+dw-padding &&
                    e.getY()>=fy && e.getY()<=fy+fieldH) {
                    focusedField = i;
                    panel.repaint(); return;
                }
                fy += fieldH+8;
            }
            int btnY = fy+8;
            int bw2=120, bh2=32;
            // Cancel
            if (e.getX()>=dx+padding && e.getX()<=dx+padding+bw2 &&
                e.getY()>=btnY && e.getY()<=btnY+bh2) {
                callback.accept(null); panel.repaint(); return;
            }
            // OK
            if (e.getX()>=dx+dw-padding-bw2 && e.getX()<=dx+dw-padding &&
                e.getY()>=btnY && e.getY()<=btnY+bh2) {
                callback.accept(values); panel.repaint();
            }
        }
    }
}