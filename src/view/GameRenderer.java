package view;

import controller.GameController;
import model.*;
import network.GameSnapshot;
import network.LobbyState;
import util.*;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.List;

/**
 * Stateless renderer. Renders both single-player (via GameController) and
 * multiplayer states (via GameSnapshot / LobbyState).
 */
public class GameRenderer {

    // ── Palette ──────────────────────────────────────────────
    private static final Color BG           = new Color(5,   5,   20);
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
    private static final Color ACCENT_GOLD  = new Color(255, 215,   0);
    private static final Color ACCENT_RED   = new Color(226,  75,  74);
    private static final Color ACCENT_BLUE  = new Color(55,  138, 221);
    private static final Color PANEL_BG     = new Color(13,  13,  34);
    private static final Color PANEL_BORDER = new Color(26,  26,  62);
    private static final Color TEXT_DIM     = new Color(136, 136, 136);
    private static final Color TEXT_MID     = new Color(200, 200, 200);
    private static final Color GREEN_DOT    = new Color(99,  153,  34);
    private static final Color ERROR_BG     = new Color(26,   5,   5);
    private static final Color ERROR_BORDER = new Color(163,  45,  45);

    // ── Fonts ────────────────────────────────────────────────
    private static final Font TITLE_FONT  = new Font("Courier New", Font.BOLD, 48);
    private static final Font SUB_FONT    = new Font("Courier New", Font.BOLD, 24);
    private static final Font BIG_FONT    = new Font("Courier New", Font.BOLD, 32);
    private static final Font MED_FONT    = new Font("Courier New", Font.BOLD, 16);
    private static final Font HUD_FONT    = new Font("Courier New", Font.BOLD, 16);
    private static final Font HINT_FONT   = new Font("Courier New", Font.PLAIN, 13);
    private static final Font SMALL_FONT  = new Font("Courier New", Font.PLAIN, 11);
    private static final Font LABEL_FONT  = new Font("Courier New", Font.BOLD, 10);

    private int animTick = 0;
    private static final int T = GameConfig.TILE_SIZE;

    // ── Single-Player Entry Point ────────────────────────────

    public void render(Graphics2D g2, GameController ctrl, int w, int h) {
        animTick++;
        GameState state = ctrl.getState();
        renderMaze(g2, ctrl.getMaze());
        renderPellets(g2, ctrl.getMaze());

        if (state != GameState.START) {
            renderChasers(g2, ctrl.getChasers());
            renderPlayerFromModel(g2, ctrl.getPlayer(), state, ctrl.getDeathAnimTicks());
            renderHUD(g2, ctrl.getPlayer(), w);
        }

        switch (state) {
            case START:     drawStartScreen(g2, w, h);           break;
            case PAUSED:    drawPauseOverlay(g2, w, h);          break;
            case DEAD:      drawDeathFlash(g2, w, h, ctrl.getDeathAnimTicks()); break;
            case GAME_OVER: drawEndOverlay(g2, w, h, false, "");  break;
            case WIN:       drawEndOverlay(g2, w, h, true, "");   break;
            default: break;
        }
    }

    // ── Multiplayer Entry Point ──────────────────────────────

    public void renderMultiplayer(Graphics2D g2, int w, int h,
                                   GameState uiState,
                                   GameSnapshot snap,
                                   LobbyState lobby,
                                   String myName,
                                   Role myRole,
                                   String errorMsg,
                                   String serverAddr,
                                   int countdown,
                                   boolean isHost) {
        animTick++;
        g2.setColor(BG);
        g2.fillRect(0, 0, w, h);

        switch (uiState) {
            case START:
                drawM2StartScreen(g2, w, h);
                break;
            case MP_MENU:
                drawMultiplayerMenuScreen(g2, w, h);
                break;
            case JOINING:
                drawJoiningScreen(g2, w, h, serverAddr);
                break;
            case LOBBY:
                drawLobbyScreen(g2, w, h, lobby, myName, serverAddr, isHost);
                break;
            case ROLE_REVEAL:
                drawRoleRevealScreen(g2, w, h, myRole, lobby, myName);
                break;
            case COUNTDOWN:
                drawCountdownScreen(g2, w, h, countdown, lobby, myName);
                break;
            case PLAYING:
            case DEAD:
            case PAUSED:
                if (snap != null) {
                    renderSnapshotGame(g2, w, h, snap, uiState, myName);
                }
                break;
            case GAME_OVER:
                if (snap != null) renderSnapshotGame(g2, w, h, snap, uiState, myName);
                drawEndOverlay(g2, w, h, false, snap != null ? snap.winnerName : "");
                break;
            case WIN:
                if (snap != null) renderSnapshotGame(g2, w, h, snap, uiState, myName);
                drawEndOverlay(g2, w, h, true, snap != null ? snap.winnerName : "");
                break;
            case ERROR:
                drawErrorScreen(g2, w, h, errorMsg, serverAddr);
                break;
            case DISCONNECTED:
                if (snap != null) renderSnapshotGame(g2, w, h, snap, GameState.PLAYING, myName);
                drawDisconnectedOverlay(g2, w, h, serverAddr);
                break;
            default:
                break;
        }
    }

    // ── Snapshot-based Game Rendering ────────────────────────

    private void renderSnapshotGame(Graphics2D g2, int w, int h, GameSnapshot snap, GameState state, String myName) {
        // Rebuild a lightweight maze for rendering (walls only; pellets from snap)
        renderMazeGrid(g2);
        renderSnapshotPellets(g2, snap);
        renderSnapshotChasers(g2, snap);
        renderSnapshotChomper(g2, snap, state);
        renderSnapshotHUD(g2, snap, w, myName);

        if (state == GameState.DEAD) {
            drawDeathFlash(g2, w, h, snap.deathTicks);
        }
    }

    private void renderMazeGrid(Graphics2D g2) {
        // Re-create the maze grid for rendering (same as Maze.java hardcoded grid)
        int[][] grid = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,1,0,1},
            {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
            {1,0,0,0,0,0,1,0,1,0,0,0,0,0,1},
            {1,0,1,1,1,0,1,0,1,0,1,1,1,0,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,1,0,1},
            {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
            {1,0,0,0,0,0,1,0,1,0,0,0,0,0,1},
            {1,0,1,1,1,0,0,0,0,0,1,1,1,0,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        };
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                int x = c * T, y = r * T;
                if (grid[r][c] == 1) {
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

    private void renderSnapshotPellets(Graphics2D g2, GameSnapshot snap) {
        // All path cells; use snap.pelletsCollected mask
        // We need to know which cells are path — use the same pellet order as Maze.buildPellets()
        int[][] grid = getMazeGrid();
        int[][] powerPos = {{1,1},{1,13},{13,1},{13,13}};
        int pelletIdx = 0;
        boolean[] mask = snap.pelletsCollected;
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (grid[r][c] != 0) continue;
                boolean collected = (mask != null && pelletIdx < mask.length) ? mask[pelletIdx] : false;
                pelletIdx++;
                if (collected) continue;
                boolean isPower = false;
                for (int[] pp : powerPos) if (pp[0]==r && pp[1]==c) { isPower=true; break; }
                int x = c * T, y = r * T;
                if (isPower) {
                    double pulse = 1.0 + 0.3 * Math.sin(animTick * 0.2);
                    int sz = (int)(14 * pulse);
                    int ox = x + T/2 - sz/2, oy = y + T/2 - sz/2;
                    g2.setColor(new Color(255, 180, 80, 110));
                    g2.fillOval(ox-4, oy-4, sz+8, sz+8);
                    g2.setColor(POWER_CLR);
                    g2.fillOval(ox, oy, sz, sz);
                } else {
                    g2.setColor(PELLET_CLR);
                    g2.fillOval(x+17, y+17, 6, 6);
                }
            }
        }
    }

    private void renderSnapshotChasers(Graphics2D g2, GameSnapshot snap) {
        if (snap.chasers == null) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (GameSnapshot.ChaserData cd : snap.chasers) {
            if ("EATEN".equals(cd.mode)) continue;
            boolean frightened = "FRIGHTENED".equals(cd.mode);
            boolean flash = frightened && cd.frightenTicks < 8 && (animTick % 4 < 2);
            Color body = frightened ? (flash ? FLASH_CLR : FRIGHT_CLR) : CHASER_CLR;
            drawGhost(g2, cd.row, cd.col, body, frightened, cd.name, cd.human);
        }
    }

    private void renderSnapshotChomper(Graphics2D g2, GameSnapshot snap, GameState state) {
        int x  = snap.chomperCol * T + 2;
        int y  = snap.chomperRow * T + 2;
        int sz = T - 4;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.DEAD) {
            float ratio  = snap.deathTicks / (float) GameConfig.DEATH_TICKS;
            int   shrunk = (int)(sz * ratio);
            int   off    = (sz - shrunk) / 2;
            g2.setColor(PLAYER_CLR);
            g2.fillOval(x + off, y + off, shrunk, shrunk);
            return;
        }

        double mouthDeg = (snap.chomperMouthFrame % 2 == 0) ? 40 : 10;
        double rotation;
        switch (snap.chomperDir != null ? snap.chomperDir : "NONE") {
            case "LEFT":  rotation = 180; break;
            case "UP":    rotation = 270; break;
            case "DOWN":  rotation =  90; break;
            default:      rotation =   0; break;
        }
        Graphics2D g3 = (Graphics2D) g2.create();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.translate(x + sz/2, y + sz/2);
        g3.rotate(Math.toRadians(rotation));
        if (snap.chomperPowered) {
            int ga = 100 + (int)(80 * Math.sin(animTick * 0.3));
            g3.setColor(new Color(255, 120, 0, ga));
            g3.fillOval(-sz/2-5, -sz/2-5, sz+10, sz+10);
        }
        g3.setColor(PLAYER_CLR);
        g3.fillArc(-sz/2, -sz/2, sz, sz, (int)(mouthDeg/2), (int)(360-mouthDeg));
        g3.setColor(PLAYER_EYE);
        g3.fillOval(-2, -sz/2+5, 5, 5);
        g3.dispose();

        // Name label
        drawNameLabel(g2, snap.chomperName, snap.chomperRow, snap.chomperCol, ACCENT_GOLD);
    }

    private void renderSnapshotHUD(Graphics2D g2, GameSnapshot snap, int panelW, String myName) {
        g2.setColor(HUD_BG);
        g2.fillRect(0, 0, panelW, 24);
        g2.setFont(HUD_FONT);
        g2.setColor(Color.WHITE);
        g2.drawString("SCORE: " + snap.chomperScore, 8, 17);
        g2.drawString("LIVES:", panelW - 145, 17);
        for (int i = 0; i < snap.chomperLives; i++) {
            g2.setColor(PLAYER_CLR);
            g2.fillOval(panelW - 65 + i * 20, 5, 13, 13);
        }
        if (snap.chomperPowered) {
            float pct  = snap.chomperPowerTicks / (float) GameConfig.POWER_DURATION;
            int   barW = (int)(80 * pct);
            int   bx   = panelW/2 - 40;
            g2.setColor(new Color(255, 80, 0, 170));
            g2.fillRect(bx, 6, barW, 10);
            g2.setColor(POWER_CLR);
            g2.drawRect(bx, 6, 80, 10);
        }
    }

    // ── Single-player rendering (same as M1) ─────────────────

    private void renderMaze(Graphics2D g2, Maze maze) {
        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getCols(); c++) {
                int x = c*T, y = r*T;
                if (maze.getCell(r,c) == Maze.WALL) {
                    g2.setColor(WALL_DARK); g2.fillRect(x, y, T, T);
                    g2.setColor(WALL_LIGHT); g2.drawRect(x+1, y+1, T-3, T-3);
                } else {
                    g2.setColor(PATH_COLOR); g2.fillRect(x, y, T, T);
                }
            }
        }
    }

    private void renderPellets(Graphics2D g2, Maze maze) {
        for (Pellet p : maze.getPellets()) {
            if (p.isCollected()) continue;
            int x = p.getCol()*T, y = p.getRow()*T;
            if (p.isPower()) {
                double pulse = 1.0 + 0.3*Math.sin(animTick*0.2);
                int sz = (int)(14*pulse);
                int ox = x+T/2-sz/2, oy = y+T/2-sz/2;
                g2.setColor(new Color(255,180,80,110)); g2.fillOval(ox-4,oy-4,sz+8,sz+8);
                g2.setColor(POWER_CLR); g2.fillOval(ox,oy,sz,sz);
            } else {
                g2.setColor(PELLET_CLR); g2.fillOval(x+17,y+17,6,6);
            }
        }
    }

    private void renderPlayerFromModel(Graphics2D g2, Player p, GameState st, int deathTicks) {
        int x = p.getCol()*T+2, y = p.getRow()*T+2, sz = T-4;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (st == GameState.DEAD) {
            float ratio = deathTicks/(float)GameConfig.DEATH_TICKS;
            int shrunk = (int)(sz*ratio), off = (sz-shrunk)/2;
            g2.setColor(PLAYER_CLR); g2.fillOval(x+off, y+off, shrunk, shrunk);
            return;
        }
        double mouthDeg = (p.getMouthFrame()%2==0) ? 40 : 10;
        double rotation;
        switch (p.getDirection()) {
            case LEFT: rotation=180; break; case UP: rotation=270; break;
            case DOWN: rotation=90;  break; default: rotation=0;   break;
        }
        Graphics2D g3 = (Graphics2D)g2.create();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.translate(x+sz/2, y+sz/2);
        g3.rotate(Math.toRadians(rotation));
        if (p.isPowered()) {
            int ga = 100+(int)(80*Math.sin(animTick*0.3));
            g3.setColor(new Color(255,120,0,ga)); g3.fillOval(-sz/2-5,-sz/2-5,sz+10,sz+10);
        }
        g3.setColor(PLAYER_CLR);
        g3.fillArc(-sz/2,-sz/2,sz,sz,(int)(mouthDeg/2),(int)(360-mouthDeg));
        g3.setColor(PLAYER_EYE); g3.fillOval(-2,-sz/2+5,5,5);
        g3.dispose();
    }

    private void renderChasers(Graphics2D g2, List<Chaser> chasers) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Chaser c : chasers) {
            if (c.isEaten()) continue;
            boolean flash = c.isFrightened() && c.getFrightenedTicks()<8 && (animTick%4<2);
            Color body = c.isFrightened() ? (flash ? FLASH_CLR : FRIGHT_CLR) : CHASER_CLR;
            drawGhost(g2, c.getRow(), c.getCol(), body, c.isFrightened(), null, false);
        }
    }

    private void renderHUD(Graphics2D g2, Player p, int panelW) {
        g2.setColor(HUD_BG); g2.fillRect(0,0,panelW,24);
        g2.setFont(HUD_FONT); g2.setColor(Color.WHITE);
        g2.drawString("SCORE: "+p.getScore(), 8, 17);
        g2.drawString("LIVES:", panelW-145, 17);
        for (int i=0; i<p.getLives(); i++) { g2.setColor(PLAYER_CLR); g2.fillOval(panelW-65+i*20,5,13,13); }
        if (p.isPowered()) {
            float pct=(float)p.getPowerTicks()/GameConfig.POWER_DURATION;
            int barW=(int)(80*pct), bx=panelW/2-40;
            g2.setColor(new Color(255,80,0,170)); g2.fillRect(bx,6,barW,10);
            g2.setColor(POWER_CLR); g2.drawRect(bx,6,80,10);
        }
    }

    // ── Ghost drawing ────────────────────────────────────────

    private void drawGhost(Graphics2D g2, int row, int col, Color body, boolean frightened, String name, boolean isHuman) {
        int x = col*T+2, y = row*T+2, sz = T-4;
        g2.setColor(body);
        g2.fillArc(x, y, sz, sz, 0, 180);
        g2.fillRect(x, y+sz/2, sz, sz/2);
        int skirtY = y+sz, segW = sz/3;
        g2.setColor(PATH_COLOR);
        for (int i=0; i<3; i++) g2.fillArc(x+i*segW, skirtY-5, segW, 10, 0, -180);
        if (!frightened) {
            g2.setColor(Color.WHITE);
            g2.fillOval(x+5,y+6,8,8); g2.fillOval(x+sz-13,y+6,8,8);
            g2.setColor(new Color(0,0,200));
            g2.fillOval(x+7,y+8,4,4); g2.fillOval(x+sz-11,y+8,4,4);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillOval(x+5,y+9,6,5); g2.fillOval(x+sz-11,y+9,6,5);
            g2.drawArc(x+6,y+sz/2-2,5,5,0,-180);
            g2.drawArc(x+12,y+sz/2-2,5,5,0,180);
            g2.drawArc(x+18,y+sz/2-2,5,5,0,-180);
        }
        if (isHuman && name != null) {
            drawNameLabel(g2, name, row, col, ACCENT_RED);
        }
    }

    private void drawNameLabel(Graphics2D g2, String name, int row, int col, Color color) {
        if (name == null || name.isEmpty()) return;
        g2.setFont(new Font("Courier New", Font.BOLD, 9));
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(name);
        int lx = col*T + T/2 - lw/2;
        int ly = row*T - 2;
        g2.setColor(new Color(0,0,0,160));
        g2.fillRect(lx-2, ly-10, lw+4, 12);
        g2.setColor(color);
        g2.drawString(name, lx, ly);
    }

    // ── M2 Screens ───────────────────────────────────────────

    /** Main title with Single Player / Multiplayer */
    private void drawM2StartScreen(Graphics2D g2, int w, int h) {
        g2.setColor(BG); g2.fillRect(0,0,w,h);
        // Title
        g2.setFont(TITLE_FONT); g2.setColor(ACCENT_GOLD);
        drawCentered(g2, "CHOMPERS", w, h/2 - 90);
        g2.setFont(new Font("Courier New", Font.BOLD, 28)); g2.setColor(TEXT_MID);
        drawCentered(g2, "& CHASERS", w, h/2 - 50);

        // Buttons area — drawn as colored rectangles
        int bw = 260, bh = 50, bx = w/2 - bw/2;

        // Single Player button (gold)
        g2.setColor(ACCENT_GOLD);
        g2.fillRoundRect(bx, h/2 - 30, bw, bh, 6, 6);
        g2.setFont(BIG_FONT); g2.setColor(Color.BLACK);
        drawCenteredY(g2, "SINGLE PLAYER", bx, h/2-30, bw, bh);

        // Multiplayer button (blue)
        g2.setColor(ACCENT_BLUE);
        g2.fillRoundRect(bx, h/2 + 40, bw, bh, 6, 6);
        g2.setFont(BIG_FONT); g2.setColor(Color.WHITE);
        drawCenteredY(g2, "MULTIPLAYER", bx, h/2+40, bw, bh);

        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "Arrow Keys  ·  P pause  ·  R restart  ·  ESC quit", w, h/2 + 148);
    }

    /** Multiplayer menu with Host Game / Join Game */
    private void drawMultiplayerMenuScreen(Graphics2D g2, int w, int h) {
        g2.setColor(BG); g2.fillRect(0, 0, w, h);
        g2.setFont(MED_FONT); g2.setColor(ACCENT_GOLD);
        drawCentered(g2, "MULTIPLAYER", w, h/2 - 100);

        int bw = 260, bh = 50, bx = w/2 - bw/2, spacing = 80;

        // Host a Game button (gold)
        g2.setColor(ACCENT_GOLD);
        g2.fillRoundRect(bx, h/2 - 30, bw, bh, 6, 6);
        g2.setFont(BIG_FONT); g2.setColor(Color.BLACK);
        drawCenteredY(g2, "HOST A GAME", bx, h/2 - 30, bw, bh);

        // Join a Game button (blue)
        g2.setColor(ACCENT_BLUE);
        g2.fillRoundRect(bx, h/2 + 40, bw, bh, 6, 6);
        g2.setFont(BIG_FONT); g2.setColor(Color.WHITE);
        drawCenteredY(g2, "JOIN A GAME", bx, h/2 + 40, bw, bh);

        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "ESC to go back", w, h - 50);
    }

    /** Mode selection screen (1v1, 1v2, 1v3) */
    /** "Connecting…" spinner screen */
    private void drawJoiningScreen(Graphics2D g2, int w, int h, String addr) {
        g2.setFont(MED_FONT); g2.setColor(TEXT_MID);
        drawCentered(g2, "Connecting...", w, h/2 - 20);
        // Spinner dots
        for (int i=0; i<3; i++) {
            int alpha = (animTick % 6 == i) ? 255 : 80;
            g2.setColor(new Color(255,215,0,alpha));
            g2.fillOval(w/2-20+i*20, h/2+5, 10, 10);
        }
        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, addr, w, h/2+35);
    }

    /** Lobby waiting screen */
    private void drawLobbyScreen(Graphics2D g2, int w, int h, LobbyState lobby,
                                   String myName, String addr, boolean isHost) {
        int cx = w/2;
        // Header
        g2.setFont(MED_FONT); g2.setColor(TEXT_MID);
        drawCentered(g2, "Waiting for players...", w, 60);

        // Connection status dot
        int dotX = cx - 80, dotY = 75;
        g2.setColor(GREEN_DOT); g2.fillOval(dotX, dotY, 8, 8);
        g2.setFont(SMALL_FONT); g2.setColor(new Color(151,196,89));
        g2.drawString("Connected  " + addr, dotX+14, dotY+8);

        // Player count
        int count = lobby != null ? lobby.players.size() : 0;
        g2.setFont(LABEL_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "PLAYERS  (" + count + " / " + GameConfig.MAX_PLAYERS + ")", w, 108);

        // Player slots
        int slotW = 320, slotH = 36;
        int slotX = cx - slotW/2;
        int slotY = 116;

        for (int i = 0; i < GameConfig.MAX_PLAYERS; i++) {
            boolean filled = lobby != null && i < lobby.players.size();
            LobbyState.PlayerInfo pi = filled ? lobby.players.get(i) : null;

            // slot background
            g2.setColor(PANEL_BG); g2.fillRoundRect(slotX, slotY, slotW, slotH, 6, 6);
            g2.setColor(PANEL_BORDER); g2.drawRoundRect(slotX, slotY, slotW, slotH, 6, 6);

            if (filled) {
                // dot
                g2.setColor(new Color(68,68,68)); g2.fillOval(slotX+10, slotY+13, 10, 10);
                // name
                g2.setFont(SMALL_FONT); g2.setColor(new Color(204,204,204));
                String label = pi.name;
                boolean isMe = pi.name.equals(myName);
                g2.drawString(label, slotX+28, slotY+22);
                // YOU badge
                if (isMe) {
                    drawBadge(g2, "you", slotX+28+g2.getFontMetrics().stringWidth(label)+6, slotY+10,
                              new Color(26,58,26), new Color(151,196,89), new Color(59,109,17));
                }
                // HOST badge
                boolean isHostPlayer = lobby != null && pi.id == lobby.hostId;
                if (isHostPlayer) {
                    int bx2 = slotX+28+g2.getFontMetrics().stringWidth(label)+6+(isMe?42:0);
                    drawBadge(g2, "host", bx2, slotY+10, new Color(26,26,62), new Color(133,183,235), new Color(24,95,165));
                }
                // TBD role
                g2.setFont(SMALL_FONT); g2.setColor(new Color(68,68,68));
                g2.drawString("TBD", slotX+slotW-42, slotY+22);
            } else {
                // empty slot
                g2.setColor(new Color(68,68,68)); // dashed dot approximation
                g2.drawOval(slotX+10, slotY+13, 10, 10);
                g2.setFont(SMALL_FONT); g2.setColor(new Color(68,68,68));
                g2.drawString("waiting...", slotX+28, slotY+22);
            }

            slotY += slotH + 6;
        }

        // Role mode indicators
        int modeY = slotY + 8;
        int modeW = 80, modeH = 44, modeGap = 8;
        int totalModeW = 3*(modeW+modeGap)-modeGap;
        int modeX = cx - totalModeW/2;
        String[][] modes = {{"1","CHOMPER","3","CHASERS"},{"2","CHOMPERS","2","CHASERS"}};
        // Just show 1-3 mode highlighted
        g2.setColor(new Color(26,20,0)); g2.fillRoundRect(modeX, modeY, modeW*2+modeGap, modeH, 6, 6);
        g2.setColor(ACCENT_GOLD); g2.drawRoundRect(modeX, modeY, modeW*2+modeGap, modeH, 6, 6);
        g2.setFont(new Font("Courier New", Font.BOLD, 18)); g2.setColor(ACCENT_GOLD);
        drawCenteredY(g2, "1-3", modeX, modeY, modeW*2+modeGap, modeH/2);
        g2.setFont(SMALL_FONT); g2.setColor(TEXT_DIM);
        drawCenteredY(g2, "active mode", modeX, modeY+modeH/2, modeW*2+modeGap, modeH/2);

        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "Host can start when 4 players have joined", w, modeY+modeH+20);

        // Start button (host only, or greyed)
        if (isHost) {
            int btnW=200, btnH=34, btnX=cx-btnW/2, btnY=modeY+modeH+36;
            boolean canStart = count >= 1;
            g2.setColor(canStart ? new Color(50,50,70) : new Color(30,30,40));
            g2.fillRoundRect(btnX,btnY,btnW,btnH,6,6);
            g2.setColor(canStart ? new Color(100,100,160) : new Color(60,60,80));
            g2.drawRoundRect(btnX,btnY,btnW,btnH,6,6);
            g2.setFont(MED_FONT); g2.setColor(canStart ? TEXT_MID : new Color(80,80,80));
            drawCenteredY(g2, "START GAME (HOST)", btnX, btnY, btnW, btnH);
        }
    }

    /** Role reveal screen */
    private void drawRoleRevealScreen(Graphics2D g2, int w, int h, Role myRole, LobbyState lobby, String myName) {
        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "Your role has been assigned", w, 55);

        boolean isChomper = myRole == Role.CHOMPER;
        int cardW=260, cardH=120, cardX=w/2-cardW/2, cardY=70;
        Color cardBorder = isChomper ? ACCENT_GOLD : ACCENT_RED;
        Color cardBg     = isChomper ? new Color(26,22,0) : new Color(26,5,5);

        g2.setColor(cardBg); g2.fillRoundRect(cardX,cardY,cardW,cardH,10,10);
        g2.setColor(cardBorder); g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(cardX,cardY,cardW,cardH,10,10);
        g2.setStroke(new BasicStroke(1));

        // Icon
        g2.setFont(new Font("Courier New", Font.BOLD, 36)); g2.setColor(cardBorder);
        drawCentered(g2, isChomper ? "●" : "◆", w, cardY+50);
        // Role name
        g2.setFont(new Font("Courier New", Font.BOLD, 22)); g2.setColor(cardBorder);
        drawCentered(g2, isChomper ? "CHOMPER" : "CHASER", w, cardY+78);
        // Desc
        g2.setFont(SMALL_FONT); g2.setColor(TEXT_DIM);
        String desc1 = isChomper ? "Collect all pellets to win." : "Catch the Chomper to win.";
        String desc2 = isChomper ? "Grab power pellets to eat Chasers!" : "Use BFS instinct or arrow keys!";
        drawCentered(g2, desc1, w, cardY+98);
        drawCentered(g2, desc2, w, cardY+112);

        // All players list
        if (lobby != null && !lobby.players.isEmpty()) {
            g2.setFont(LABEL_FONT); g2.setColor(TEXT_DIM);
            drawCentered(g2, "ALL PLAYERS", w, cardY+cardH+28);
            int slotW=300, slotH=32, slotX=w/2-slotW/2, slotY=cardY+cardH+36;
            for (LobbyState.PlayerInfo pi : lobby.players) {
                g2.setColor(PANEL_BG); g2.fillRoundRect(slotX,slotY,slotW,slotH,6,6);
                g2.setColor(PANEL_BORDER); g2.drawRoundRect(slotX,slotY,slotW,slotH,6,6);
                boolean isChomperRole = "CHOMPER".equals(pi.role);
                g2.setColor(isChomperRole ? ACCENT_GOLD : ACCENT_RED);
                g2.fillOval(slotX+10, slotY+11, 10, 10);
                g2.setFont(SMALL_FONT); g2.setColor(TEXT_MID);
                String displayName = pi.name.equals(myName) ? pi.name+" (you)" : pi.name;
                g2.drawString(displayName, slotX+28, slotY+21);
                g2.setColor(isChomperRole ? ACCENT_GOLD : ACCENT_RED);
                g2.drawString(pi.role, slotX+slotW-80, slotY+21);
                slotY += slotH+4;
            }
        }

        // Ready button
        int bw=200,bh=36,bx2=w/2-bw/2,by2=h-60;
        g2.setColor(ACCENT_GOLD); g2.fillRoundRect(bx2,by2,bw,bh,6,6);
        g2.setFont(MED_FONT); g2.setColor(Color.BLACK);
        drawCenteredY(g2,"READY",bx2,by2,bw,bh);
    }

    /** Countdown screen */
    private void drawCountdownScreen(Graphics2D g2, int w, int h, int countdown, LobbyState lobby, String myName) {
        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "All players ready — game starting in", w, 65);

        // Ring
        int cx=w/2, cy=130, r2=40;
        g2.setColor(ACCENT_GOLD); g2.setStroke(new BasicStroke(3));
        g2.drawOval(cx-r2, cy-r2, r2*2, r2*2);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Courier New", Font.BOLD, 36)); g2.setColor(ACCENT_GOLD);
        String num = countdown > 0 ? String.valueOf(countdown) : "GO!";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(num, cx - fm.stringWidth(num)/2, cy + fm.getAscent()/2 - 2);

        // Player list
        if (lobby != null) {
            int slotW=300, slotH=32, slotX=w/2-slotW/2, slotY=190;
            for (LobbyState.PlayerInfo pi : lobby.players) {
                boolean isChomperRole = "CHOMPER".equals(pi.role);
                g2.setColor(PANEL_BG); g2.fillRoundRect(slotX,slotY,slotW,slotH,6,6);
                g2.setColor(PANEL_BORDER); g2.drawRoundRect(slotX,slotY,slotW,slotH,6,6);
                g2.setColor(isChomperRole ? ACCENT_GOLD : ACCENT_RED);
                g2.fillOval(slotX+10, slotY+11, 10, 10);
                g2.setFont(SMALL_FONT); g2.setColor(TEXT_MID);
                String displayName = pi.name.equals(myName) ? pi.name+" (you)" : pi.name;
                g2.drawString(displayName, slotX+28, slotY+21);
                g2.setFont(SMALL_FONT); g2.setColor(GREEN_DOT);
                g2.drawString("● ready", slotX+slotW-70, slotY+21);
                slotY += slotH+4;
            }
        }
        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "Server controls the countdown timer", w, h-30);
    }

    /** Error screen */
    private void drawErrorScreen(Graphics2D g2, int w, int h, String msg, String addr) {
        g2.setFont(new Font("Courier New",Font.BOLD,11)); g2.setColor(TEXT_DIM);
        String label = addr != null && !addr.isEmpty() ? "CONNECTION REFUSED" : "ERROR";
        drawCentered(g2, label, w, h/2-80);

        int bw=300, bh=80, bx=w/2-bw/2, by=h/2-65;
        g2.setColor(ERROR_BG); g2.fillRoundRect(bx,by,bw,bh,6,6);
        g2.setColor(ERROR_BORDER); g2.drawRoundRect(bx,by,bw,bh,6,6);
        g2.setFont(new Font("Courier New",Font.BOLD,13)); g2.setColor(new Color(240,149,149));
        drawCentered(g2, "Cannot connect to server", w, by+22);
        g2.setFont(SMALL_FONT); g2.setColor(new Color(240,149,149));
        // word-wrap the message
        String display = msg != null ? msg : "Unknown error";
        drawCentered(g2, display, w, by+46);
        if (addr != null) drawCentered(g2, addr, w, by+62);

        // Try again button
        int btnW=200,btnH=34,btnX=w/2-btnW/2,btnY=h/2+30;
        g2.setColor(new Color(50,50,70)); g2.fillRoundRect(btnX,btnY,btnW,btnH,6,6);
        g2.setColor(new Color(100,100,140)); g2.drawRoundRect(btnX,btnY,btnW,btnH,6,6);
        g2.setFont(MED_FONT); g2.setColor(TEXT_MID);
        drawCenteredY(g2,"TRY AGAIN",btnX,btnY,btnW,btnH);
    }

    /** Disconnected overlay (rendered over frozen game) */
    private void drawDisconnectedOverlay(Graphics2D g2, int w, int h, String addr) {
        // Dim overlay
        g2.setColor(new Color(0,0,0,160)); g2.fillRect(0,0,w,h);

        int bw=300, bh=160, bx=w/2-bw/2, by=h/2-bh/2;
        g2.setColor(new Color(10,0,0,220)); g2.fillRoundRect(bx,by,bw,bh,8,8);
        g2.setColor(ERROR_BORDER); g2.drawRoundRect(bx,by,bw,bh,8,8);

        // Red dot + text
        g2.setColor(ACCENT_RED); g2.fillOval(bx+20, by+20, 8, 8);
        g2.setFont(SMALL_FONT); g2.setColor(new Color(240,149,149));
        g2.drawString("Connection lost", bx+34, by+28);

        g2.setFont(MED_FONT); g2.setColor(Color.WHITE);
        drawCentered(g2, "Disconnected from server", w, by+60);
        g2.setFont(SMALL_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, addr != null ? addr : "", w, by+82);

        // Progress bar (animated)
        int pct = (animTick*3) % 100;
        int pbW=220, pbH=6, pbX=w/2-pbW/2, pbY=by+96;
        g2.setColor(new Color(26,26,46)); g2.fillRoundRect(pbX,pbY,pbW,pbH,3,3);
        g2.setColor(ACCENT_BLUE); g2.fillRoundRect(pbX,pbY,(int)(pbW*pct/100.0),pbH,3,3);

        // Buttons
        int btn1W=110,btn1H=30,btn1X=w/2-btn1W-6,btn1Y=by+bh-48;
        g2.setColor(new Color(50,50,70)); g2.fillRoundRect(btn1X,btn1Y,btn1W,btn1H,4,4);
        g2.setColor(new Color(100,100,140)); g2.drawRoundRect(btn1X,btn1Y,btn1W,btn1H,4,4);
        g2.setFont(SMALL_FONT); g2.setColor(TEXT_MID);
        drawCenteredY(g2,"Reconnect",btn1X,btn1Y,btn1W,btn1H);

        int btn2W=110,btn2H=30,btn2X=w/2+6,btn2Y=by+bh-48;
        g2.setColor(new Color(26,5,5)); g2.fillRoundRect(btn2X,btn2Y,btn2W,btn2H,4,4);
        g2.setColor(ERROR_BORDER); g2.drawRoundRect(btn2X,btn2Y,btn2W,btn2H,4,4);
        g2.setColor(ACCENT_RED);
        drawCenteredY(g2,"Quit",btn2X,btn2Y,btn2W,btn2H);
    }

    // ── Common overlays ──────────────────────────────────────

    private void drawStartScreen(Graphics2D g2, int w, int h) {
        // Single-player start (M1 style)
        g2.setColor(new Color(0,0,0,215)); g2.fillRect(0,0,w,h);
        g2.setFont(TITLE_FONT); g2.setColor(PLAYER_CLR);
        drawCentered(g2, "CHOMPERS", w, h/2-60);
        g2.setFont(BIG_FONT); g2.setColor(TEXT_MID);
        drawCentered(g2, "& CHASERS", w, h/2-15);
        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        String[] tips = {"Arrow Keys  —  Move","P  —  Pause / Resume","R  —  Restart anytime","Collect POWER PELLETS (orange) to eat Chasers!"};
        int hy = h/2+22;
        for (String t : tips) { drawCentered(g2,t,w,hy); hy+=22; }
        if ((animTick/10)%2==0) {
            g2.setFont(HUD_FONT); g2.setColor(PLAYER_CLR);
            drawCentered(g2, ">>  PRESS ENTER TO START  <<", w, h/2+130);
        }
    }

    private void drawPauseOverlay(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0,0,0,160)); g2.fillRect(0,0,w,h);
        g2.setFont(BIG_FONT); g2.setColor(Color.WHITE);
        drawCentered(g2,"PAUSED",w,h/2-10);
        g2.setFont(HINT_FONT); g2.setColor(TEXT_MID);
        drawCentered(g2,"P — resume  |  R — restart",w,h/2+28);
    }

    private void drawEndOverlay(Graphics2D g2, int w, int h, boolean win, String winnerName) {
        Color bg = win ? new Color(0,30,0,200) : new Color(30,0,0,200);
        g2.setColor(bg); g2.fillRect(0,0,w,h);
        g2.setFont(BIG_FONT); g2.setColor(Color.WHITE);
        drawCentered(g2, win ? "YOU WIN!" : "GAME OVER", w, h/2-20);
        if (winnerName != null && !winnerName.isEmpty()) {
            g2.setFont(MED_FONT); g2.setColor(TEXT_MID);
            drawCentered(g2, win ? "Winner: "+winnerName : "Chasers win!", w, h/2+14);
        }
        g2.setFont(HINT_FONT); g2.setColor(TEXT_DIM);
        drawCentered(g2, "R — play again  |  ESC — quit", w, h/2+46);
    }

    private void drawDeathFlash(Graphics2D g2, int w, int h, int ticks) {
        int alpha = (int)(80*(ticks/(float)GameConfig.DEATH_TICKS));
        g2.setColor(new Color(255,0,0,alpha)); g2.fillRect(0,0,w,h);
    }

    // ── Utilities ────────────────────────────────────────────

    private void drawCentered(Graphics2D g2, String text, int w, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (w - fm.stringWidth(text))/2, y);
    }

    private void drawCenteredY(Graphics2D g2, String text, int bx, int by, int bw, int bh) {
        FontMetrics fm = g2.getFontMetrics();
        int tx = bx + (bw - fm.stringWidth(text))/2;
        int ty = by + (bh - fm.getHeight())/2 + fm.getAscent();
        g2.drawString(text, tx, ty);
    }

    private void drawBadge(Graphics2D g2, String text, int bx, int by, Color bg, Color fg, Color border) {
        g2.setFont(SMALL_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(text)+10, bh=16;
        g2.setColor(bg); g2.fillRoundRect(bx,by,bw,bh,3,3);
        g2.setColor(border); g2.drawRoundRect(bx,by,bw,bh,3,3);
        g2.setColor(fg); g2.drawString(text, bx+5, by+11);
    }

    private int[][] getMazeGrid() {
        return new int[][] {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,1,0,1},
            {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
            {1,0,0,0,0,0,1,0,1,0,0,0,0,0,1},
            {1,0,1,1,1,0,1,0,1,0,1,1,1,0,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,1,0,1},
            {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
            {1,0,0,0,0,0,1,0,1,0,0,0,0,0,1},
            {1,0,1,1,1,0,0,0,0,0,1,1,1,0,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        };
    }
}