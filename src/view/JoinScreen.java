package view;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class JoinScreen extends JPanel {
    private final JTextField nameField;
    private final JTextField ipField;

    public JoinScreen(Consumer<String[]> onConnect, Runnable onBack) {
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,10,8,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("JOIN GAME", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 28));
        title.setForeground(Color.YELLOW);
        add(title, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        JLabel nameLabel = new JLabel("Your Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Courier New", Font.PLAIN, 16));
        add(nameLabel, gbc);
        gbc.gridx = 1;
        nameField = new JTextField("Player", 15);
        nameField.setFont(new Font("Courier New", Font.PLAIN, 16));
        add(nameField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        JLabel ipLabel = new JLabel("Host IP:");
        ipLabel.setForeground(Color.WHITE);
        ipLabel.setFont(new Font("Courier New", Font.PLAIN, 16));
        add(ipLabel, gbc);
        gbc.gridx = 1;
        ipField = new JTextField("192.168.1.1", 15);
        ipField.setFont(new Font("Courier New", Font.PLAIN, 16));
        add(ipField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        JButton connectBtn = new JButton("Connect");
        connectBtn.setFont(new Font("Courier New", Font.BOLD, 18));
        connectBtn.setBackground(new Color(0,180,100));
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setFocusPainted(false);
        connectBtn.setPreferredSize(new Dimension(260, 50));
        connectBtn.addActionListener(e -> onConnect.accept(new String[]{nameField.getText().trim(), ipField.getText().trim()}));
        add(connectBtn, gbc);

        gbc.gridy++;
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 16));
        backBtn.setBackground(new Color(100,100,100));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setPreferredSize(new Dimension(260, 40));
        backBtn.addActionListener(e -> onBack.run());
        add(backBtn, gbc);
    }
}
