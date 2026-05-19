package view;

import network.NetworkUtils;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class HostScreen extends JPanel {
    private final JTextField nameField;

    public HostScreen(Consumer<String> onHost, Runnable onBack) {
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("HOST GAME", SwingConstants.CENTER);
        title.setFont(new Font("Courier New", Font.BOLD, 28));
        title.setForeground(Color.YELLOW);
        add(title, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        JLabel nameLabel = new JLabel("Your Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Courier New", Font.PLAIN, 16));
        add(nameLabel, gbc);
        gbc.gridx = 1;
        nameField = new JTextField("Host", 15);
        nameField.setFont(new Font("Courier New", Font.PLAIN, 16));
        add(nameField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        String ip = NetworkUtils.getLocalIP();
        JLabel ipInfo = new JLabel("<html><center>Your IP:<br><b style='font-size:18px;color:cyan'>" + ip + "</b><br>Share this with players!</center></html>", SwingConstants.CENTER);
        ipInfo.setForeground(Color.CYAN);
        ipInfo.setFont(new Font("Courier New", Font.PLAIN, 14));
        add(ipInfo, gbc);

        gbc.gridy++;
        JButton hostBtn = new JButton("Start Server & Host");
        hostBtn.setFont(new Font("Courier New", Font.BOLD, 18));
        hostBtn.setBackground(new Color(0,180,100));
        hostBtn.setForeground(Color.WHITE);
        hostBtn.setFocusPainted(false);
        hostBtn.setPreferredSize(new Dimension(280, 50));
        hostBtn.addActionListener(e -> onHost.accept(nameField.getText().trim()));
        add(hostBtn, gbc);

        gbc.gridy++;
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 16));
        backBtn.setBackground(new Color(100,100,100));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setPreferredSize(new Dimension(280, 40));
        backBtn.addActionListener(e -> onBack.run());
        add(backBtn, gbc);
    }
}
