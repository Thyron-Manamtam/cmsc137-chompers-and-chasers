package view;

import controller.GameController;
import network.*;
import util.*;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private MainMenuPanel         mainMenu;
    private HowToPlayScreen       howToPlay;
    private ModeSelectScreen      modeSelect;
    private MultiplayerMenuScreen mpMenu;
    private HostScreen            hostScreen;
    private JoinScreen            joinScreen;
    private LobbyScreen           lobbyScreen;
    private RoleSelectScreen      roleSelect;
    private RoleRevealScreen      roleReveal;
    private GamePanel             singlePlayerPanel;
    private MultiplayerGamePanel  mpGamePanel;

    private GameClient client;
    private GameServer server;
    private Thread serverThread;
    private boolean isHost;
    private String currentCard = "mainMenu";

    public GameWindow() {
        setTitle("Chompers & Chasers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        root.setBackground(Color.BLACK);
        add(root);

        buildMainMenu();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        showCard("mainMenu");
    }

    private void buildMainMenu() {
        mainMenu = new MainMenuPanel(
            () -> { buildModeSelect(); showCard("modeSelect"); },
            () -> { buildHowToPlay(); showCard("howToPlay"); },
            () -> System.exit(0)
        );
        root.add(mainMenu, "mainMenu");
    }

    private void buildHowToPlay() {
        if (howToPlay != null) { showCard("howToPlay"); return; }
        howToPlay = new HowToPlayScreen(() -> showCard("mainMenu"));
        root.add(howToPlay, "howToPlay");
    }

    private void buildModeSelect() {
        if (modeSelect != null) { showCard("modeSelect"); return; }
        modeSelect = new ModeSelectScreen(
            this::startSinglePlayer,
            () -> { buildMpMenu(); showCard("mpMenu"); }
        );
        root.add(modeSelect, "modeSelect");
    }

    private void buildMpMenu() {
        if (mpMenu != null) { showCard("mpMenu"); return; }
        mpMenu = new MultiplayerMenuScreen(
            this::showHostScreen,
            this::showJoinScreen,
            () -> showCard("modeSelect")
        );
        root.add(mpMenu, "mpMenu");
    }

    private void showHostScreen() {
        hostScreen = new HostScreen(this::startHosting, () -> showCard("mpMenu"));
        root.add(hostScreen, "hostScreen");
        setSize2(GameConfig.GAME_W, GameConfig.GAME_H);
        showCard("hostScreen");
    }

    private void showJoinScreen() {
        joinScreen = new JoinScreen(parts -> joinGame(parts[0], parts[1]), () -> showCard("mpMenu"));
        root.add(joinScreen, "joinScreen");
        setSize2(GameConfig.GAME_W, GameConfig.GAME_H);
        showCard("joinScreen");
    }

    // ── Single Player ────────────────────────────────────────

    private void startSinglePlayer() {
        singlePlayerPanel = new GamePanel();
        root.add(singlePlayerPanel, "singlePlayer");
        setSize2(GameConfig.GAME_W, GameConfig.GAME_H);
        showCard("singlePlayer");
        singlePlayerPanel.requestFocusInWindow();
    }

    // ── Host ─────────────────────────────────────────────────

    private void startHosting(String name) {
        isHost = true;

        // 1. Create and bind the ServerSocket first (fast, no network round-trip)
        try {
            server = new GameServer(GameConfig.SERVER_PORT);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Port " + GameConfig.SERVER_PORT + " is busy.\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Start acceptLoop in background thread BEFORE connecting the client.
        //    This fixes the race condition where the client tried to connect before
        //    acceptLoop() was running, causing "connection refused" on the host itself.
        serverThread = new Thread(() -> server.acceptLoop(), "ServerAcceptLoop");
        serverThread.setDaemon(true);
        serverThread.start();

        // 3. Give acceptLoop a moment to actually start listening
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        // 4. Now connect the host's own client in a background thread (not EDT)
        String displayName = name.isEmpty() ? "Host" : name;
        client = new GameClient();
        setupClientCallbacks();

        showConnecting("Starting server...");

        new Thread(() -> {
            // Host connects to their own server via localhost
            boolean ok = client.connect("127.0.0.1", GameConfig.SERVER_PORT, displayName);
            SwingUtilities.invokeLater(() -> {
                if (!ok) {
                    JOptionPane.showMessageDialog(this,
                        "Could not connect to local server.\nTry restarting the game.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    showCard("mpMenu");
                } else {
                    showLobby(true, NetworkUtils.getLocalIP());
                }
            });
        }, "HostConnectThread").start();
    }

    // ── Join ─────────────────────────────────────────────────

    private void joinGame(String name, String ip) {
        String trimmedIp = ip.trim();
        String displayName = name.isEmpty() ? "Player" : name;

        // Prevent joining your own IP — this would create a second server
        // and connect to it alone, not to the host's server.
        String myIp = NetworkUtils.getLocalIP();
        if (trimmedIp.equals(myIp) || trimmedIp.equals("127.0.0.1") || trimmedIp.equals("localhost")) {
            JOptionPane.showMessageDialog(this,
                "That is your own IP address (" + trimmedIp + ").\n" +
                "To host a game, use 'Host Game' instead.\n" +
                "To join someone else's game, enter THEIR IP address.",
                "Wrong IP", JOptionPane.WARNING_MESSAGE);
            return;
        }

        isHost = false;
        client = new GameClient();
        setupClientCallbacks();

        showConnecting("Connecting to " + trimmedIp + "...");

        // Connect off the EDT so the UI doesn't freeze
        new Thread(() -> {
            boolean ok = client.connect(trimmedIp, GameConfig.SERVER_PORT, displayName);
            SwingUtilities.invokeLater(() -> {
                if (!ok) {
                    JOptionPane.showMessageDialog(this,
                        "Cannot connect to " + trimmedIp + ":" + GameConfig.SERVER_PORT + "\n\n" +
                        "Checklist:\n" +
                        "  1. Did the host click 'Start Server & Host'?\n" +
                        "  2. Is the IP correct? Host should see their IP on the Host screen.\n" +
                        "  3. Are both laptops on the SAME WiFi network?\n" +
                        "  4. Is Windows Firewall blocking Java? (most common cause)\n" +
                        "     → Open Windows Defender Firewall\n" +
                        "     → Click 'Allow an app through firewall'\n" +
                        "     → Find Java and check both Private and Public boxes.",
                        "Connection Failed", JOptionPane.ERROR_MESSAGE);
                    showCard("joinScreen");
                } else {
                    showLobby(false, trimmedIp);
                }
            });
        }, "JoinConnectThread").start();
    }

    // ── Connecting placeholder screen ─────────────────────────

    private void showConnecting(String message) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.BLACK);
        JLabel lbl = new JLabel(message, SwingConstants.CENTER);
        lbl.setFont(new Font("Courier New", Font.BOLD, 18));
        lbl.setForeground(Color.CYAN);
        p.add(lbl);
        root.add(p, "connecting");
        showCard("connecting");
    }

    // ── Lobby ────────────────────────────────────────────────

    private void showLobby(boolean host, String ip) {
        SwingUtilities.invokeLater(() -> {
            lobbyScreen = new LobbyScreen(host, client.getState().myId, ip,
                this::showRoleSelect,
                () -> { client.disconnect(); showCard("mpMenu"); }
            );
            root.add(lobbyScreen, "lobby");
            setSize2(GameConfig.GAME_W, GameConfig.GAME_H);
            showCard("lobby");
        });
    }

    // ── Role Select ──────────────────────────────────────────

    private void showRoleSelect() {
        roleSelect = new RoleSelectScreen(
            role -> {
                client.sendRole(role);
                client.sendReady();
                if (isHost) client.sendStart();
            },
            () -> showCard("lobby")
        );
        root.add(roleSelect, "roleSelect");
        setSize2(GameConfig.GAME_W, GameConfig.GAME_H);
        showCard("roleSelect");
        roleSelect.requestFocusInWindow();
    }

    // ── Role Reveal ──────────────────────────────────────────

    private void showRoleReveal(Role role) {
        SwingUtilities.invokeLater(() -> {
            if (roleReveal != null) roleReveal.stopCountdown();
            roleReveal = new RoleRevealScreen(role, this::startMultiplayerGame);
            root.add(roleReveal, "roleReveal");
            setSize2(GameConfig.GAME_W, GameConfig.GAME_H);
            showCard("roleReveal");
            roleReveal.requestFocusInWindow();
        });
    }

    // ── Multiplayer Game ─────────────────────────────────────

    private void startMultiplayerGame() {
        SwingUtilities.invokeLater(() -> {
            ClientGameState state = client.getState();
            mpGamePanel = new MultiplayerGamePanel(client, state, () -> {});
            root.add(mpGamePanel, "mpGame");
            setSize2(GameConfig.GAME_W, GameConfig.GAME_H);
            showCard("mpGame");
            mpGamePanel.requestFocusInWindow();

            client.setOnStateUpdate(s -> {
                SwingUtilities.invokeLater(() -> {
                    if (mpGamePanel != null) mpGamePanel.updateState(s);
                });
            });
        });
    }

    // ── Client callbacks ─────────────────────────────────────

    private void setupClientCallbacks() {
        client.setOnStateUpdate(s -> {
            SwingUtilities.invokeLater(() -> {
                if (lobbyScreen != null && currentCard.equals("lobby")) {
                    lobbyScreen.updatePlayers(s.players);
                }
            });
        });

        client.setOnGameStart(() -> {
            Role r = client.getState().myRole;
            if (r != null) showRoleReveal(r);
            else startMultiplayerGame();
        });

        client.setOnError(msg -> {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE));
        });

        client.setOnGameOver(() -> {
            if (mpGamePanel != null) {
                SwingUtilities.invokeLater(() -> mpGamePanel.updateState(client.getState()));
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────

    private void showCard(String name) {
        currentCard = name;
        cards.show(root, name);
    }

    private void setSize2(int w, int h) {
        setPreferredSize(new Dimension(w, h));
        pack();
    }

    public static void launch() {
        SwingUtilities.invokeLater(GameWindow::new);
    }
}