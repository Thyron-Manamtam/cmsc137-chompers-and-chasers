package view;

import util.GameConfig;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HowToPlayScreen extends JPanel {

    public HowToPlayScreen(Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(GameConfig.GAME_W, GameConfig.GAME_H));

        // Title
        JLabel title = new JLabel("HOW TO PLAY", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 30));
        title.setForeground(Color.YELLOW);
        title.setBorder(new EmptyBorder(18, 0, 8, 0));
        add(title, BorderLayout.NORTH);

        // Scrollable content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.BLACK);
        content.setBorder(new EmptyBorder(0, 30, 10, 30));

        addSection(content, "CONTROLS",
            "Arrow Keys — Move your character\n" +
            "P — Pause / Resume (single player)\n" +
            "R — Restart (single player)\n" +
            "ESC — Quit");

        addSection(content, "SINGLE PLAYER",
            "You are the Chomper.\n" +
            "Collect ALL pellets to win!\n" +
            "Avoid the 4 AI Chasers chasing you.\n" +
            "Orange Power Pellets let you eat Chasers temporarily!");

        addSection(content, "MULTIPLAYER (LAN) — HOST",
            "Go to Play → Multiplayer → Host Game.\n" +
            "Enter your name, click \"Start Server & Host\".\n" +
            "Your IP address will be shown — share it!\n" +
            "Wait for players to join in the Lobby.");

        addSection(content, "MULTIPLAYER (LAN) — JOIN",
            "Go to Play → Multiplayer → Join Game.\n" +
            "Enter your name and the HOST'S IP address.\n" +
            "Click Connect and wait in the Lobby.");

        addSection(content, "ROLE SELECT (ALL PLAYERS)",
            "Every player must choose CHOMPER or CHASER.\n" +
            "Click READY — you cannot change your role after!\n" +
            "If multiple players pick Chomper, one is chosen randomly.\n" +
            "Game automatically starts 3 seconds after ALL players are ready.\n" +
            "Supports 2–5 players: 1 Chomper + 1 to 4 Chasers.");

        addSection(content, "POWER PELLETS (Orange)",
            "When the Chomper eats an orange Power Pellet:\n" +
            "  → Chasers turn blue and can be eaten!\n" +
            "  → If Chomper touches a blue Chaser, that Chaser is ELIMINATED.\n" +
            "  → Eliminated Chasers do NOT respawn — they are gone for good!\n" +
            "  → Eating more Chasers in one power-up scores bonus points!");

        addSection(content, "GAMEPLAY",
            "All players share the same maze view.\n" +
            "Chomper: collect pellets, use power pellets wisely!\n" +
            "Chasers: catch the Chomper or outlast the 2-minute timer.\n" +
            "Timer: 2 minutes — if it expires, Chasers win.");

        addSection(content, "WIN CONDITIONS",
            "CHOMPERS WIN if:\n" +
            "  → All pellets are collected, OR\n" +
            "  → All Chasers are eaten (via Power Pellets)!\n" +
            "\n" +
            "CHASERS WIN if:\n" +
            "  → Chomper loses all 3 lives, OR\n" +
            "  → The 2-minute timer expires.");

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBackground(Color.BLACK);
        scroll.getViewport().setBackground(Color.BLACK);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50,50,80), 1));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Back button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        bottom.setBackground(Color.BLACK);
        JButton backBtn = new JButton("← Back");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 18));
        backBtn.setBackground(new Color(60,60,80));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setPreferredSize(new Dimension(200, 48));
        backBtn.addActionListener(e -> onBack.run());
        bottom.add(backBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addSection(JPanel parent, String heading, String body) {
        JLabel headLabel = new JLabel(heading);
        headLabel.setFont(new Font("Courier New", Font.BOLD, 16));
        headLabel.setForeground(new Color(80, 200, 255));
        headLabel.setAlignmentX(LEFT_ALIGNMENT);
        headLabel.setBorder(new EmptyBorder(14, 0, 4, 0));
        parent.add(headLabel);

        for (String line : body.split("\n")) {
            // Empty line = small spacer
            if (line.trim().isEmpty()) {
                parent.add(Box.createVerticalStrut(6));
                continue;
            }
            JLabel lbl = new JLabel("  " + line);
            lbl.setFont(new Font("Courier New", Font.PLAIN, 14));
            // Highlight key mechanic lines in orange
            if (line.contains("→") || line.contains("ELIMINATED") || line.contains("do NOT respawn")) {
                lbl.setForeground(new Color(255, 180, 60));
            } else if (line.startsWith("CHOMPERS WIN") || line.startsWith("CHASERS WIN")) {
                lbl.setForeground(new Color(140, 255, 140));
            } else {
                lbl.setForeground(new Color(210, 210, 210));
            }
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            parent.add(lbl);
        }

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(50, 50, 70));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        parent.add(sep);
    }
}