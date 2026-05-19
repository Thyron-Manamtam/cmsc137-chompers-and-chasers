package view;

import util.Role;
import javax.swing.*;
import java.awt.*;

public class RoleRevealScreen extends JPanel {
    private final Timer countdown;
    private int secondsLeft;
    private final JLabel countdownLabel;

    public RoleRevealScreen(Role role, Runnable onDone) {
        setLayout(new GridBagLayout());
        boolean isChomper = role == Role.CHOMPER;
        setBackground(isChomper ? new Color(0,30,60) : new Color(60,0,0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15,20,15,20);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel youAre = new JLabel("YOU ARE A", SwingConstants.CENTER);
        youAre.setFont(new Font("Courier New", Font.BOLD, 28));
        youAre.setForeground(Color.LIGHT_GRAY);
        add(youAre, gbc);

        gbc.gridy++;
        JLabel roleLabel = new JLabel(role.name(), SwingConstants.CENTER);
        roleLabel.setFont(new Font("Courier New", Font.BOLD, 52));
        roleLabel.setForeground(isChomper ? Color.YELLOW : new Color(255,80,80));
        add(roleLabel, gbc);

        gbc.gridy++;
        String objective = isChomper
            ? "<html><center>Collect all pellets and avoid the Chasers!<br>Use POWER PELLETS to turn the tables!</center></html>"
            : "<html><center>Catch the Chomper before they clear the maze!<br>You win if time runs out or Chomper loses all lives!</center></html>";
        JLabel objLabel = new JLabel(objective, SwingConstants.CENTER);
        objLabel.setFont(new Font("Courier New", Font.PLAIN, 15));
        objLabel.setForeground(Color.WHITE);
        add(objLabel, gbc);

        gbc.gridy++;
        countdownLabel = new JLabel("Starting in 3...", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Courier New", Font.BOLD, 18));
        countdownLabel.setForeground(Color.CYAN);
        add(countdownLabel, gbc);

        gbc.gridy++;
        JLabel hint = new JLabel("Press SPACE to skip", SwingConstants.CENTER);
        hint.setFont(new Font("Courier New", Font.PLAIN, 13));
        hint.setForeground(Color.GRAY);
        add(hint, gbc);

        secondsLeft = 3;
        countdown = new Timer(1000, e -> {
            secondsLeft--;
            if (secondsLeft <= 0) {
                ((Timer)e.getSource()).stop();
                onDone.run();
            } else {
                countdownLabel.setText("Starting in " + secondsLeft + "...");
            }
        });
        countdown.start();

        // SPACE to skip
        setFocusable(true);
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    countdown.stop();
                    onDone.run();
                }
            }
        });
    }

    public void stopCountdown() {
        if (countdown != null) countdown.stop();
    }
}
