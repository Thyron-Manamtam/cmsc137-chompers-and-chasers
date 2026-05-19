package view;

import network.NetworkUtils;
import javax.swing.*;
import java.awt.*;

public class MultiplayerMenuScreen extends JPanel {
    public MultiplayerMenuScreen(Runnable onHost, Runnable onJoin, Runnable onBack) {
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("MULTIPLAYER", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 28));
        title.setForeground(Color.YELLOW);
        add(title, gbc);

        gbc.gridy++;
        JButton hostBtn = makeButton("Host Game", new Color(0,180,100));
        hostBtn.addActionListener(e -> onHost.run());
        add(hostBtn, gbc);

        gbc.gridy++;
        JButton joinBtn = makeButton("Join Game", new Color(0,100,220));
        joinBtn.addActionListener(e -> onJoin.run());
        add(joinBtn, gbc);

        gbc.gridy++;
        JButton backBtn = makeButton("Back", new Color(100,100,100));
        backBtn.addActionListener(e -> onBack.run());
        add(backBtn, gbc);
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier New", Font.BOLD, 18));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(260, 50));
        return b;
    }
}
