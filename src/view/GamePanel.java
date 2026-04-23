package view;

import controller.GameController;
import util.Direction;
import util.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The main game canvas. Handles input and delegates rendering to GameRenderer.
 * Keeps no game state — only the controller does.
 */
public class GamePanel extends JPanel implements KeyListener {
    private static final int TILE = 40;
    private static final int COLS = 15;
    private static final int ROWS = 15;

    private final GameController controller;
    private final GameRenderer renderer;

    public GamePanel() {
        setPreferredSize(new Dimension(COLS * TILE, ROWS * TILE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        renderer = new GameRenderer();
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

    /** Called by controller to add bonus score (e.g. eating a chaser). */
    public void addScore(int bonus) {
        // Score is managed on Player; this hook is for future sound/effects
    }

    public void showMessage(String msg) {
        // Replaced by in-canvas overlays — kept for compatibility
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    controller.setPlayerDirection(Direction.UP);    break;
            case KeyEvent.VK_DOWN:  controller.setPlayerDirection(Direction.DOWN);  break;
            case KeyEvent.VK_LEFT:  controller.setPlayerDirection(Direction.LEFT);  break;
            case KeyEvent.VK_RIGHT: controller.setPlayerDirection(Direction.RIGHT); break;

            case KeyEvent.VK_ENTER:
                if (controller.getState() == GameState.START) controller.startGame();
                break;

            case KeyEvent.VK_P:
                controller.togglePause();
                break;

            case KeyEvent.VK_R:
                controller.restartGame();
                break;

            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
