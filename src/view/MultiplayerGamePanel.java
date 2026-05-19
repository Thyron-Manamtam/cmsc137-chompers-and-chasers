package view;

import network.ClientGameState;
import network.GameClient;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MultiplayerGamePanel extends JPanel implements KeyListener {
    private static final int T    = GameConfig.TILE_SIZE;
    private static final int ROWS = GameConfig.MAZE_ROWS;
    private static final int COLS = GameConfig.MAZE_COLS;

    private final GameClient client;
    private volatile ClientGameState state;
    private Runnable onGameOver;
    private int animTick = 0;

    // Countdown overlay before game starts
    private int startCountdown = 3;
    private boolean countingDown = true;

    private static final int[][] GRID = {
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

    private static final Color[] PLAYER_COLORS = {
        Color.YELLOW, new Color(255,80,80), Color.CYAN, Color.GREEN, Color.MAGENTA
    };

    public MultiplayerGamePanel(GameClient client, ClientGameState initialState, Runnable onGameOver) {
        this.client     = client;
        this.state      = initialState;
        this.onGameOver = onGameOver;

        setPreferredSize(new Dimension(COLS * T, ROWS * T + 30));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Countdown timer: 3-2-1 GO
        Timer cdTimer = new Timer(1000, null);
        cdTimer.addActionListener(e -> {
            startCountdown--;
            if (startCountdown <= 0) {
                countingDown = false;
                cdTimer.stop();
            }
            repaint();
        });
        cdTimer.start();

        // Repaint timer
        Timer repaintTimer = new Timer(GameConfig.TICK_MS, e -> { animTick++; repaint(); });
        repaintTimer.start();
    }

    public void updateState(ClientGameState newState) {
        this.state = newState;
        if (newState.gameResult != null) SwingUtilities.invokeLater(onGameOver);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ClientGameState s = state;
        if (s == null) return;

        drawMaze(g2);
        drawPellets(g2, s);
        drawPlayers(g2, s);
        drawHUD(g2, s);

        if (countingDown) drawCountdownOverlay(g2, startCountdown);
        else if (s.gameResult != null) drawResult(g2, s);
    }

    private void drawMaze(Graphics2D g2) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = c*T, y = r*T+30;
                if (GRID[r][c] == 1) {
                    g2.setColor(new Color(0,50,170)); g2.fillRect(x,y,T,T);
                    g2.setColor(new Color(20,90,220)); g2.drawRect(x+1,y+1,T-3,T-3);
                } else {
                    g2.setColor(new Color(5,5,20)); g2.fillRect(x,y,T,T);
                }
            }
        }
    }

    private void drawPellets(Graphics2D g2, ClientGameState s) {
        for (ClientGameState.PelletInfo p : s.pellets) {
            if (p.collected) continue;
            int x = p.col*T, y = p.row*T+30;
            if (p.power) {
                double pulse = 1.0 + 0.3 * Math.sin(animTick * 0.2);
                int sz = (int)(14*pulse);
                g2.setColor(new Color(255,100,50));
                g2.fillOval(x+T/2-sz/2, y+T/2-sz/2, sz, sz);
            } else {
                g2.setColor(new Color(255,220,140)); g2.fillOval(x+17,y+17,6,6);
            }
        }
    }

    private void drawPlayers(Graphics2D g2, ClientGameState s) {
        int colorIdx = 0;
        for (ClientGameState.PlayerInfo p : s.players) {
            Color col = PLAYER_COLORS[colorIdx++ % PLAYER_COLORS.length];
            int x = p.col*T+2, y = p.row*T+2+30, sz = T-4;

            if (p.role == util.Role.CHOMPER) {
                int mouth = (animTick%2==0) ? 40 : 10;
                double rot = 0;
                if (p.direction != null) switch (p.direction) {
                    case "LEFT": rot=180; break; case "UP": rot=270; break; case "DOWN": rot=90; break;
                }
                Graphics2D g3 = (Graphics2D)g2.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g3.translate(x+sz/2, y+sz/2);
                g3.rotate(Math.toRadians(rot));
                if (p.powered) { g3.setColor(new Color(255,120,0,120)); g3.fillOval(-sz/2-5,-sz/2-5,sz+10,sz+10); }
                g3.setColor(col); g3.fillArc(-sz/2,-sz/2,sz,sz,(int)(mouth/2),(int)(360-mouth));
                g3.dispose();
                g2.setFont(new Font("Courier New", Font.PLAIN, 10));
                g2.setColor(Color.WHITE); g2.drawString(p.name, x, y-2);
            } else {
                Color body = p.powered ? new Color(30,60,200) : col;
                g2.setColor(body);
                g2.fillArc(x,y,sz,sz,0,180); g2.fillRect(x,y+sz/2,sz,sz/2);
                g2.setColor(new Color(5,5,20));
                int segW = sz/3;
                for (int i = 0; i < 3; i++) g2.fillArc(x+i*segW,y+sz-5,segW,10,0,-180);
                g2.setColor(Color.WHITE);
                g2.fillOval(x+5,y+6,8,8); g2.fillOval(x+sz-13,y+6,8,8);
                g2.setColor(new Color(0,0,200));
                g2.fillOval(x+7,y+8,4,4); g2.fillOval(x+sz-11,y+8,4,4);
                g2.setFont(new Font("Courier New", Font.PLAIN, 10));
                g2.setColor(Color.WHITE); g2.drawString(p.name, x, y-2);
            }
        }
    }

    private void drawHUD(Graphics2D g2, ClientGameState s) {
        g2.setColor(new Color(0,0,0,200)); g2.fillRect(0,0,getWidth(),30);
        int mins = s.timeLeft/60, secs = s.timeLeft%60;
        String timerStr = String.format("TIME %d:%02d", mins, secs);
        g2.setFont(new Font("Courier New", Font.BOLD, 16));
        g2.setColor(s.timeLeft < 30 ? Color.RED : Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(timerStr, getWidth()/2 - fm.stringWidth(timerStr)/2, 20);

        int x = 5; int colorIdx = 0;
        g2.setFont(new Font("Courier New", Font.BOLD, 12));
        for (ClientGameState.PlayerInfo p : s.players) {
            Color col = PLAYER_COLORS[colorIdx++ % PLAYER_COLORS.length];
            g2.setColor(col);
            String info = p.name + ":" + p.score;
            if (p.role == util.Role.CHOMPER) info += " ♥" + p.lives;
            g2.drawString(info, x, 20);
            x += g2.getFontMetrics().stringWidth(info) + 12;
        }
    }

    private void drawCountdownOverlay(Graphics2D g2, int count) {
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(0,0,0,160)); g2.fillRect(0,0,w,h);
        g2.setFont(new Font("Courier New", Font.BOLD, 80));
        String txt = count > 0 ? String.valueOf(count) : "GO!";
        Color c = count > 0 ? Color.CYAN : Color.YELLOW;
        g2.setColor(c);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, (w-fm.stringWidth(txt))/2, h/2+30);
        g2.setFont(new Font("Courier New", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        String sub = "Get ready!";
        fm = g2.getFontMetrics();
        g2.drawString(sub, (w-fm.stringWidth(sub))/2, h/2+80);
    }

    private void drawResult(Graphics2D g2, ClientGameState s) {
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(0,0,0,180)); g2.fillRect(0,0,w,h);
        g2.setFont(new Font("Courier New", Font.BOLD, 36));
        String winner = "CHOMPERS".equals(s.gameResult) ? "CHOMPERS WIN!" : "CHASERS WIN!";
        g2.setColor("CHOMPERS".equals(s.gameResult) ? Color.YELLOW : new Color(255,80,80));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(winner, (w-fm.stringWidth(winner))/2, h/2);
        g2.setFont(new Font("Courier New", Font.PLAIN, 18));
        g2.setColor(Color.WHITE);
        String sub = "Reason: " + s.resultReason;
        fm = g2.getFontMetrics();
        g2.drawString(sub, (w-fm.stringWidth(sub))/2, h/2+35);
        g2.setFont(new Font("Courier New", Font.PLAIN, 14));
        g2.setColor(Color.LIGHT_GRAY);
        String hint = "ESC to exit";
        fm = g2.getFontMetrics();
        g2.drawString(hint, (w-fm.stringWidth(hint))/2, h/2+65);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Direction d = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    d = Direction.UP;    break;
            case KeyEvent.VK_DOWN:  d = Direction.DOWN;  break;
            case KeyEvent.VK_LEFT:  d = Direction.LEFT;  break;
            case KeyEvent.VK_RIGHT: d = Direction.RIGHT; break;
            case KeyEvent.VK_ESCAPE: System.exit(0); break;
        }
        if (d != null && !countingDown) client.sendInput(d);
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}