package view;

import javax.swing.*;
import java.awt.*;

public class ModeSelectScreen extends JPanel {
    public ModeSelectScreen(Runnable onSinglePlayer, Runnable onMultiplayer) {
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("SELECT MODE", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 32));
        title.setForeground(Color.YELLOW);
        add(title, gbc);

        gbc.gridy++;
        JButton spBtn = makeButton("Single Player", new Color(0,180,200));
        spBtn.addActionListener(e -> onSinglePlayer.run());
        add(spBtn, gbc);

        gbc.gridy++;
        JButton mpBtn = makeButton("Multiplayer (LAN)", new Color(220,50,100));
        mpBtn.addActionListener(e -> onMultiplayer.run());
        add(mpBtn, gbc);
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
