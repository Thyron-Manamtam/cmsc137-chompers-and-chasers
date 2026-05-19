package view;

import network.ClientGameState;
import network.NetworkUtils;
import util.Role;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LobbyScreen extends JPanel {
    private final JPanel playerListPanel;
    private final JButton startBtn;
    private final JLabel statusLabel;
    private boolean isHost;
    private int myId;
    private Runnable onStart;
    private Runnable onBack;
    private String hostIp;

    public LobbyScreen(boolean isHost, int myId, String hostIp, Runnable onStart, Runnable onBack) {
        this.isHost = isHost;
        this.myId   = myId;
        this.hostIp = hostIp;
        this.onStart = onStart;
        this.onBack  = onBack;

        setLayout(new BorderLayout(10,10));
        setBackground(Color.BLACK);

        JLabel title = new JLabel("MULTIPLAYER LOBBY", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 26));
        title.setForeground(Color.YELLOW);
        title.setBorder(BorderFactory.createEmptyBorder(15,0,5,0));
        add(title, BorderLayout.NORTH);

        // IP display
        if (isHost) {
            JLabel ipLabel = new JLabel("YOUR IP: " + hostIp + "   (share this with others)", SwingConstants.CENTER);
            ipLabel.setFont(new Font("Courier New", Font.BOLD, 16));
            ipLabel.setForeground(Color.CYAN);
            ipLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(Color.BLACK);
            topPanel.add(title, BorderLayout.NORTH);
            topPanel.add(ipLabel, BorderLayout.CENTER);
            add(topPanel, BorderLayout.NORTH);
        }

        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        playerListPanel.setBackground(Color.BLACK);
        JScrollPane scroll = new JScrollPane(playerListPanel);
        scroll.setBackground(Color.BLACK);
        scroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER,15,10));
        bottom.setBackground(Color.BLACK);

        statusLabel = new JLabel("Waiting for players...");
        statusLabel.setFont(new Font("Courier New", Font.PLAIN, 14));
        statusLabel.setForeground(Color.LIGHT_GRAY);
        bottom.add(statusLabel);

        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 15));
        backBtn.setBackground(new Color(180,50,50));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> onBack.run());
        bottom.add(backBtn);

        startBtn = new JButton("START GAME");
        startBtn.setFont(new Font("Courier New", Font.BOLD, 15));
        startBtn.setBackground(new Color(50,180,50));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);
        startBtn.setEnabled(false);
        startBtn.addActionListener(e -> onStart.run());
        if (isHost) bottom.add(startBtn);

        add(bottom, BorderLayout.SOUTH);
    }

    public void updatePlayers(List<ClientGameState.PlayerInfo> players) {
        SwingUtilities.invokeLater(() -> {
            playerListPanel.removeAll();
            Color[] colors = {Color.YELLOW, new Color(255,80,80), Color.CYAN, Color.GREEN, Color.MAGENTA};
            int idx = 0;
            for (ClientGameState.PlayerInfo p : players) {
                JPanel row = new JPanel(new BorderLayout(10,0));
                row.setBackground(new Color(30,30,30));
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3,5,3,5),
                    BorderFactory.createLineBorder(new Color(60,60,60))
                ));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

                JLabel bullet = new JLabel("●");
                bullet.setForeground(colors[idx % colors.length]);
                bullet.setFont(new Font("Dialog", Font.PLAIN, 20));
                bullet.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
                row.add(bullet, BorderLayout.WEST);

                String youTag = (p.id == myId) ? " (YOU)" : "";
                String roleTag = (p.role != null) ? " [" + p.role.name() + "]" : "";
                JLabel name = new JLabel(p.name + youTag + roleTag);
                name.setForeground(Color.WHITE);
                name.setFont(new Font("Courier New", Font.PLAIN, 15));
                row.add(name, BorderLayout.CENTER);

                playerListPanel.add(row);
                playerListPanel.add(Box.createVerticalStrut(4));
                idx++;
            }
            playerListPanel.revalidate();
            playerListPanel.repaint();

            int count = players.size();
            statusLabel.setText(count + " player(s) connected. Need " + Math.max(0, 2 - count) + " more to start.");
            if (isHost) startBtn.setEnabled(count >= 2);
        });
    }
}
