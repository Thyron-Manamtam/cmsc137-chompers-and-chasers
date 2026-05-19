package view;

import util.Role;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class RoleSelectScreen extends JPanel {
    private Role selected = Role.CHASER;
    private JLabel selectedLabel;

    public RoleSelectScreen(Consumer<Role> onReady, Runnable onBack) {
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("CHOOSE YOUR ROLE", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 26));
        title.setForeground(Color.YELLOW);
        add(title, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        JButton chomperBtn = makeRoleButton("CHOMPER", "Collect pellets\nAvoid Chasers\nGet power-ups", new Color(0,180,220));
        chomperBtn.addActionListener(e -> { selected = Role.CHOMPER; selectedLabel.setText("Selected: CHOMPER"); });
        add(chomperBtn, gbc);

        gbc.gridx = 1;
        JButton chaserBtn = makeRoleButton("CHASER", "Catch Chompers\nBlock pellets\nHunt the maze", new Color(220,50,80));
        chaserBtn.addActionListener(e -> { selected = Role.CHASER; selectedLabel.setText("Selected: CHASER"); });
        add(chaserBtn, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        selectedLabel = new JLabel("Selected: CHASER", SwingConstants.CENTER);
        selectedLabel.setFont(new Font("Courier New", Font.BOLD, 16));
        selectedLabel.setForeground(Color.CYAN);
        add(selectedLabel, gbc);

        gbc.gridy++;
        JLabel note = new JLabel("<html><center>Note: If multiple players pick Chomper,<br>one is chosen randomly.</center></html>", SwingConstants.CENTER);
        note.setFont(new Font("Courier New", Font.PLAIN, 13));
        note.setForeground(Color.LIGHT_GRAY);
        add(note, gbc);

        gbc.gridy++;
        JButton readyBtn = new JButton("READY");
        readyBtn.setFont(new Font("Courier New", Font.BOLD, 20));
        readyBtn.setBackground(new Color(0,200,100));
        readyBtn.setForeground(Color.WHITE);
        readyBtn.setFocusPainted(false);
        readyBtn.setPreferredSize(new Dimension(260, 55));
        readyBtn.addActionListener(e -> onReady.accept(selected));
        add(readyBtn, gbc);

        gbc.gridy++;
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Courier New", Font.PLAIN, 14));
        backBtn.setBackground(new Color(80,80,80));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setPreferredSize(new Dimension(260, 40));
        backBtn.addActionListener(e -> onBack.run());
        add(backBtn, gbc);
    }

    private JButton makeRoleButton(String role, String desc, Color bg) {
        String html = "<html><center><b>" + role + "</b><br><small>" + desc.replace("\n","<br>") + "</small></center></html>";
        JButton b = new JButton(html);
        b.setFont(new Font("Courier New", Font.PLAIN, 14));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(170, 120));
        return b;
    }
}
