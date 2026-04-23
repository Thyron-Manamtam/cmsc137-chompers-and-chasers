package view;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GameWindow extends JFrame {

    private final GamePanel gamePanel;

    public GameWindow() {
        setTitle("Chompers & Chasers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        gamePanel.requestFocusInWindow();
    }

    public static void launch() {
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
