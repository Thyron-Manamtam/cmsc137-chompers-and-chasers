package view;

import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {
    public MainMenuPanel(Runnable onPlay, Runnable onExit) {
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12,10,12,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("CHOMPERS", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 50));
        title.setForeground(Color.YELLOW);
        add(title, gbc);

        gbc.gridy++;
        JLabel sub = new JLabel("& CHASERS", SwingConstants.CENTER);
        sub.setFont(new Font("Courier New", Font.BOLD, 32));
        sub.setForeground(new Color(200,200,200));
        add(sub, gbc);

        gbc.gridy++;
        JButton playBtn = makeButton("PLAY", new Color(0,180,220));
        playBtn.addActionListener(e -> onPlay.run());
        add(playBtn, gbc);

        gbc.gridy++;
        JButton exitBtn = makeButton("EXIT", new Color(200,50,50));
        exitBtn.addActionListener(e -> onExit.run());
        add(exitBtn, gbc);
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier New", Font.BOLD, 22));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(220, 55));
        return b;
    }
}
