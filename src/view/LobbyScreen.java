package view;

import network.ClientGameState;
import util.GameConfig;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LobbyScreen extends JPanel {
    private final JPanel playerListPanel;
    private final JLabel statusLabel;
    private final JButton roleSelectBtn;
    private boolean myReady = false;
    private boolean isHost;
    private int myId;
    private Runnable onRoleSelect;
    private Runnable onBack;
    private String hostIp;

    // Countdown display
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private int countdownSecs = 3;

    public LobbyScreen(boolean isHost, int myId, String hostIp, Runnable onRoleSelect, Runnable onBack) {
        this.isHost       = isHost;
        this.myId         = myId;
        this.hostIp       = hostIp;
        this.onRoleSelect = onRoleSelect;
        this.onBack       = onBack;

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(GameConfig.GAME_W, GameConfig.GAME_H));

        // ── North: title + ip ──────────────────────────────────
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.BLACK);

        JLabel title = new JLabel("MULTIPLAYER LOBBY", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 26));
        title.setForeground(Color.YELLOW);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        topPanel.add(title);

        if (isHost) {
            JLabel ipLabel = new JLabel("YOUR IP: " + hostIp + "   (share this with others)", SwingConstants.CENTER);
            ipLabel.setFont(new Font("Courier New", Font.BOLD, 14));
            ipLabel.setForeground(Color.CYAN);
            ipLabel.setAlignmentX(CENTER_ALIGNMENT);
            topPanel.add(ipLabel);
        }

        JLabel instrLabel = new JLabel("Choose your role and click READY. Game starts when all are ready!", SwingConstants.CENTER);
        instrLabel.setFont(new Font("Courier New", Font.PLAIN, 12));
        instrLabel.setForeground(new Color(180, 180, 180));
        instrLabel.setAlignmentX(CENTER_ALIGNMENT);
        instrLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        topPanel.add(instrLabel);

        add(topPanel, BorderLayout.NORTH);

        // ── Center: player list ────────────────────────────────
        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        playerListPanel.setBackground(Color.BLACK);
        JScrollPane scroll = new JScrollPane(playerListPanel);
        scroll.setBackground(Color.BLACK);
        scroll.getViewport().setBackground(Color.BLACK);
        scroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        add(scroll, BorderLayout.CENTER);

        // ── South: status + buttons ────────────────────────────
        JPanel bottom = new JPanel(new BorderLayout(10, 8));
        bottom.setBackground(Color.BLACK);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 10, 12, 10));

        statusLabel = new JLabel("Waiting for players...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Courier New", Font.PLAIN, 14));
        statusLabel.setForeground(Color.LIGHT_GRAY);
        bottom.add(statusLabel, BorderLayout.NORTH);

        countdownLabel = new JLabel("", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Courier New", Font.BOLD, 22));
        countdownLabel.setForeground(Color.GREEN);
        countdownLabel.setVisible(false);
        bottom.add(countdownLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(Color.BLACK);

        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 15));
        backBtn.setBackground(new Color(180, 50, 50));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setPreferredSize(new Dimension(130, 44));
        backBtn.addActionListener(e -> onBack.run());
        buttonPanel.add(backBtn);

        roleSelectBtn = new JButton("CHOOSE ROLE & READY");
        roleSelectBtn.setFont(new Font("Courier New", Font.BOLD, 15));
        roleSelectBtn.setBackground(new Color(0, 160, 220));
        roleSelectBtn.setForeground(Color.WHITE);
        roleSelectBtn.setFocusPainted(false);
        roleSelectBtn.setPreferredSize(new Dimension(240, 44));
        roleSelectBtn.addActionListener(e -> {
            if (!myReady) onRoleSelect.run();
        });
        buttonPanel.add(roleSelectBtn);

        bottom.add(buttonPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    public void updatePlayers(List<ClientGameState.PlayerInfo> players) {
        SwingUtilities.invokeLater(() -> {
            playerListPanel.removeAll();
            Color[] colors = {Color.YELLOW, new Color(255, 80, 80), Color.CYAN, Color.GREEN, Color.MAGENTA};
            int idx = 0;
            int readyCount = 0;

            for (ClientGameState.PlayerInfo p : players) {
                if (p.ready) readyCount++;

                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBackground(p.ready ? new Color(20, 50, 20) : new Color(30, 30, 30));
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3, 5, 3, 5),
                    BorderFactory.createLineBorder(p.ready ? new Color(0, 120, 0) : new Color(60, 60, 60))
                ));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

                JLabel bullet = new JLabel("●");
                bullet.setForeground(colors[idx % colors.length]);
                bullet.setFont(new Font("Dialog", Font.PLAIN, 20));
                bullet.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                row.add(bullet, BorderLayout.WEST);

                String youTag  = (p.id == myId) ? " (YOU)" : "";
                String roleTag = (p.role != null) ? " [" + p.role.name() + "]" : "";
                JLabel name = new JLabel(p.name + youTag + roleTag);
                name.setForeground(Color.WHITE);
                name.setFont(new Font("Courier New", Font.PLAIN, 15));
                row.add(name, BorderLayout.CENTER);

                JLabel readyLbl = new JLabel(p.ready ? "✔ READY" : "...");
                readyLbl.setFont(new Font("Courier New", Font.BOLD, 13));
                readyLbl.setForeground(p.ready ? new Color(80, 220, 80) : Color.GRAY);
                readyLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
                row.add(readyLbl, BorderLayout.EAST);

                playerListPanel.add(row);
                playerListPanel.add(Box.createVerticalStrut(4));
                idx++;

                // Track if we ourselves are ready
                if (p.id == myId && p.ready) myReady = true;
            }

            playerListPanel.revalidate();
            playerListPanel.repaint();

            int count = players.size();
            int needMore = Math.max(0, GameConfig.MIN_PLAYERS - count);
            if (needMore > 0) {
                statusLabel.setText(count + " player(s) connected. Need " + needMore + " more to start.");
            } else {
                statusLabel.setText(readyCount + " / " + count + " players ready.");
            }

            // Update ready button state
            if (myReady) {
                roleSelectBtn.setText("✔ READY");
                roleSelectBtn.setBackground(new Color(30, 140, 30));
                roleSelectBtn.setEnabled(false);
            }
        });
    }

    /** Called when server broadcasts COUNTDOWN_START */
    public void showCountdown(int seconds) {
        SwingUtilities.invokeLater(() -> {
            countdownSecs = seconds;
            countdownLabel.setText("Starting in " + countdownSecs + "...");
            countdownLabel.setVisible(true);
            statusLabel.setText("All players ready!");

            if (countdownTimer != null) countdownTimer.stop();
            countdownTimer = new Timer(1000, e -> {
                countdownSecs--;
                if (countdownSecs <= 0) {
                    countdownLabel.setText("GO!");
                    ((Timer) e.getSource()).stop();
                } else {
                    countdownLabel.setText("Starting in " + countdownSecs + "...");
                }
            });
            countdownTimer.start();
        });
    }

    /** Called when countdown is cancelled */
    public void cancelCountdown() {
        SwingUtilities.invokeLater(() -> {
            if (countdownTimer != null) countdownTimer.stop();
            countdownLabel.setVisible(false);
            statusLabel.setText("Countdown cancelled. Waiting for players...");
        });
    }
}