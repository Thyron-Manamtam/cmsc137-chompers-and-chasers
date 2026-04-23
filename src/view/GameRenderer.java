package view;

import controller.GameController;
import model.*;
import util.GameConfig;
import util.GameState;

import java.awt.*;

/**
 * Stateless renderer. All drawing logic lives here.
 * GamePanel calls render() each repaint; this class holds no mutable game state.
 *
 * Milestone 2: render() will accept a shared GameSnapshot object sent from the
 * server instead of reading directly from the controller.
 */
public class GameRenderer {

    // ── Palette ──────────────────────────────────────────────
    private static final Color WALL_DARK    = new Color(0,   50,  170);
    private static final Color WALL_LIGHT   = new Color(20,  90,  220);
    private static final Color PATH_COLOR   = new Color(5,   5,   20);
    private static final Color PELLET_CLR   = new Color(255, 220, 140);
    private static final Color POWER_CLR    = new Color(255, 100,  50);
    private static final Color PLAYER_CLR   = new Color(255, 220,   0);
    private static final Color PLAYER_EYE   = new Color(15,  15,  15);
    private static final Color CHASER_CLR   = new Color(220,  30,  30);
    private static final Color FRIGHT_CLR   = new Color(30,   60, 200);
    private static final Color FLASH_CLR    = new Color(220, 220, 220);
    private static final Color HUD_BG       = new Color(0,    0,   0, 190);

    // ── Fonts ────────────────────────────────────────────────
    private static final Font TITLE_FONT = new Font("Courier New", Font.BOLD, 48);
    private static final Font BIG_FONT   = new Font("Courier New", Font.BOLD, 32);
    private static final Font HUD_FONT   = new Font("Courier New", Font.BOLD, 16);
    private static final Font HINT_FONT  = new Font("Courier New", Font.PLAIN, 14);

    private int animTick = 0;
    private static final int T = GameConfig.TILE_SIZE;

    // ── Entry Point ──────────────────────────────────────────

    public void render(Graphics2D g2, GameController ctrl, int w, int h) {
        animTick++;
        GameState state = ctrl.getState();

        renderMaze(g2, ctrl.getMaze());
        renderPellets(g2, ctrl.getMaze());

        if (state != GameState.START) {
            renderChasers(g2, ctrl);
            renderPlayer(g2, ctrl);
            renderHUD(g2, ctrl, w);
        }

        switch (state) {
            case START:     drawStartScreen(g2, w, h);                                             break;
            case PAUSED:    drawOverlay(g2, w, h, "PAUSED",    "P — resume  |  R — restart",
                                        new Color(0,0,0,160));                                     break;
            case DEAD:      drawDeathFlash(g2, w, h, ctrl.getDeathAnimTicks());                    break;
            case GAME_OVER: drawOverlay(g2, w, h, "GAME OVER", "R — retry  |  ESC — quit",
                                        new Color(30,0,0,200));                                    break;
            case WIN:       drawOverlay(g2, w, h, "YOU WIN!",  "R — play again",
                                        new Color(0,30,0,200));                                    break;
            default: break;
        }
    }

    // ── Maze ─────────────────────────────────────────────────

    private void renderMaze(Graphics2D g2, Maze maze) {
        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getCols(); c++) {
                int x = c * T, y = r * T;
                if (maze.getCell(r, c) == Maze.WALL) {
                    g2.setColor(WALL_DARK);
                    g2.fillRect(x, y, T, T);
                    g2.setColor(WALL_LIGHT);
                    g2.drawRect(x + 1, y + 1, T - 3, T - 3);
                } else {
                    g2.setColor(PATH_COLOR);
                    g2.fillRect(x, y, T, T);
                }
            }
        }
    }

    // ── Pellets ──────────────────────────────────────────────

    private void renderPellets(Graphics2D g2, Maze maze) {
        for (Pellet p : maze.getPellets()) {
            if (p.isCollected()) continue;
            int x = p.getCol() * T;
            int y = p.getRow() * T;

            if (p.isPower()) {
                double pulse = 1.0 + 0.3 * Math.sin(animTick * 0.2);
                int sz  = (int)(14 * pulse);
                int ox  = x + T / 2 - sz / 2;
                int oy  = y + T / 2 - sz / 2;
                g2.setColor(new Color(255, 180, 80, 110));
                g2.fillOval(ox - 4, oy - 4, sz + 8, sz + 8);
                g2.setColor(POWER_CLR);
                g2.fillOval(ox, oy, sz, sz);
            } else {
                g2.setColor(PELLET_CLR);
                g2.fillOval(x + 17, y + 17, 6, 6);
            }
        }
    }

    // ── Player (Chomper) ─────────────────────────────────────

    private void renderPlayer(Graphics2D g2, GameController ctrl) {
        Player p     = ctrl.getPlayer();
        GameState st = ctrl.getState();
        int x = p.getCol() * T + 2;
        int y = p.getRow() * T + 2;
        int sz = T - 4;

        if (st == GameState.DEAD) {
            float ratio  = ctrl.getDeathAnimTicks() / (float) GameConfig.DEATH_TICKS;
            int   shrunk = (int)(sz * ratio);
            int   off    = (sz - shrunk) / 2;
            g2.setColor(PLAYER_CLR);
            g2.fillOval(x + off, y + off, shrunk, shrunk);
            return;
        }

        double mouthDeg = (p.getMouthFrame() % 2 == 0) ? 40 : 10;
        double rotation;
        switch (p.getDirection()) {
            case LEFT:  rotation = 180; break;
            case UP:    rotation = 270; break;
            case DOWN:  rotation =  90; break;
            default:    rotation =   0; break;
        }

        Graphics2D g3 = (Graphics2D) g2.create();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.translate(x + sz / 2, y + sz / 2);
        g3.rotate(Math.toRadians(rotation));

        if (p.isPowered()) {
            int ga = 100 + (int)(80 * Math.sin(animTick * 0.3));
            g3.setColor(new Color(255, 120, 0, ga));
            g3.fillOval(-sz / 2 - 5, -sz / 2 - 5, sz + 10, sz + 10);
        }

        g3.setColor(PLAYER_CLR);
        g3.fillArc(-sz / 2, -sz / 2, sz, sz, (int)(mouthDeg / 2), (int)(360 - mouthDeg));
        g3.setColor(PLAYER_EYE);
        g3.fillOval(-2, -sz / 2 + 5, 5, 5);
        g3.dispose();
    }

    // ── Chasers ──────────────────────────────────────────────

    private void renderChasers(Graphics2D g2, GameController ctrl) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Chaser c : ctrl.getChasers()) {
            if (c.isEaten()) continue;

            int x  = c.getCol() * T + 2;
            int y  = c.getRow() * T + 2;
            int sz = T - 4;

            Color body;
            if (c.isFrightened()) {
                boolean flash = c.getFrightenedTicks() < 8 && (animTick % 4 < 2);
                body = flash ? FLASH_CLR : FRIGHT_CLR;
            } else {
                body = CHASER_CLR;
            }

            // Ghost silhouette
            g2.setColor(body);
            g2.fillArc(x, y, sz, sz, 0, 180);
            g2.fillRect(x, y + sz / 2, sz, sz / 2);

            // Wavy skirt cutouts
            int skirtY  = y + sz;
            int segW    = sz / 3;
            g2.setColor(PATH_COLOR);
            for (int i = 0; i < 3; i++)
                g2.fillArc(x + i * segW, skirtY - 5, segW, 10, 0, -180);

            // Eyes
            if (!c.isFrightened()) {
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 5,       y + 6,  8, 8);
                g2.fillOval(x + sz - 13, y + 6,  8, 8);
                g2.setColor(new Color(0, 0, 200));
                g2.fillOval(x + 7,       y + 8,  4, 4);
                g2.fillOval(x + sz - 11, y + 8,  4, 4);
            } else {
                // Scared face
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 5,       y + 9, 6, 5);
                g2.fillOval(x + sz - 11, y + 9, 6, 5);
                g2.drawArc(x + 6,  y + sz / 2 - 2, 5, 5,  0, -180);
                g2.drawArc(x + 12, y + sz / 2 - 2, 5, 5,  0,  180);
                g2.drawArc(x + 18, y + sz / 2 - 2, 5, 5,  0, -180);
            }
        }
    }

    // ── HUD ──────────────────────────────────────────────────

    private void renderHUD(Graphics2D g2, GameController ctrl, int panelW) {
        Player p = ctrl.getPlayer();

        g2.setColor(HUD_BG);
        g2.fillRect(0, 0, panelW, 24);

        g2.setFont(HUD_FONT);
        g2.setColor(Color.WHITE);
        g2.drawString("SCORE: " + p.getScore(), 8, 17);

        // Lives as mini chompers
        g2.drawString("LIVES:", panelW - 145, 17);
        for (int i = 0; i < p.getLives(); i++) {
            g2.setColor(PLAYER_CLR);
            g2.fillOval(panelW - 65 + i * 20, 5, 13, 13);
        }

        // Power bar
        if (p.isPowered()) {
            float pct  = p.getPowerTicks() / (float) GameConfig.POWER_DURATION;
            int   barW = (int)(80 * pct);
            int   bx   = panelW / 2 - 40;
            g2.setColor(new Color(255, 80, 0, 170));
            g2.fillRect(bx, 6, barW, 10);
            g2.setColor(POWER_CLR);
            g2.drawRect(bx, 6, 80, 10);
        }
    }

    // ── Screens ──────────────────────────────────────────────

    private void drawStartScreen(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 215));
        g2.fillRect(0, 0, w, h);

        g2.setFont(TITLE_FONT);
        g2.setColor(PLAYER_CLR);
        drawCentered(g2, "CHOMPERS", w, h / 2 - 60);

        g2.setFont(BIG_FONT);
        g2.setColor(new Color(200, 200, 200));
        drawCentered(g2, "& CHASERS", w, h / 2 - 15);

        g2.setFont(HINT_FONT);
        g2.setColor(new Color(170, 170, 170));
        String[] tips = {
            "Arrow Keys  —  Move",
            "P  —  Pause / Resume",
            "R  —  Restart anytime",
            "Collect POWER PELLETS (orange) to eat Chasers!",
        };
        int hy = h / 2 + 22;
        for (String t : tips) { drawCentered(g2, t, w, hy); hy += 22; }

        // Flashing prompt
        if ((animTick / 10) % 2 == 0) {
            g2.setFont(HUD_FONT);
            g2.setColor(PLAYER_CLR);
            drawCentered(g2, ">>  PRESS ENTER TO START  <<", w, h / 2 + 130);
        }
    }

    private void drawOverlay(Graphics2D g2, int w, int h,
                             String headline, String sub, Color bg) {
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        g2.setFont(BIG_FONT);
        g2.setColor(Color.WHITE);
        drawCentered(g2, headline, w, h / 2 - 10);
        g2.setFont(HINT_FONT);
        g2.setColor(new Color(200, 200, 200));
        drawCentered(g2, sub, w, h / 2 + 28);
    }

    private void drawDeathFlash(Graphics2D g2, int w, int h, int ticks) {
        int alpha = (int)(80 * (ticks / (float) GameConfig.DEATH_TICKS));
        g2.setColor(new Color(255, 0, 0, alpha));
        g2.fillRect(0, 0, w, h);
    }

    private void drawCentered(Graphics2D g2, String text, int w, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, y);
    }
}
