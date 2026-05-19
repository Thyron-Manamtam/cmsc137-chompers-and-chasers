package view;

import controller.GameController;
import util.Direction;
import util.GameState;
import util.GameConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements KeyListener {
    private static final int TILE = GameConfig.TILE_SIZE;
    private static final int COLS = GameConfig.MAZE_COLS;
    private static final int ROWS = GameConfig.MAZE_ROWS;

    private final GameController controller;
    private final GameRenderer renderer;

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.GAME_W, GameConfig.GAME_H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        renderer   = new GameRenderer();
        controller = new GameController(this);
        controller.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderer.render(g2, controller, getWidth(), getHeight());
    }

    public void addScore(int bonus) {}
    public void showMessage(String msg) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    controller.setPlayerDirection(Direction.UP);    break;
            case KeyEvent.VK_DOWN:  controller.setPlayerDirection(Direction.DOWN);  break;
            case KeyEvent.VK_LEFT:  controller.setPlayerDirection(Direction.LEFT);  break;
            case KeyEvent.VK_RIGHT: controller.setPlayerDirection(Direction.RIGHT); break;
            case KeyEvent.VK_ENTER:
                if (controller.getState() == GameState.START) controller.startGame(); break;
            case KeyEvent.VK_P: controller.togglePause(); break;
            case KeyEvent.VK_R: controller.restartGame(); break;
            case KeyEvent.VK_ESCAPE: System.exit(0); break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}