package view;

import network.ClientGameState;
import network.GameClient;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class MultiplayerGamePanel extends JPanel implements KeyListener {
    private static final int T    = GameConfig.TILE_SIZE;
    private static final int ROWS = GameConfig.MAZE_ROWS;
    private static final int COLS = GameConfig.MAZE_COLS;

    // ── Chat constants ────────────────────────────────────────────────────────
    private static final int   CHAT_MAX_MESSAGES = 6;
    private static final long  CHAT_FADE_MS      = 6_000;
    private static final int   CHAT_LINE_H       = 18;
    private static final Font  CHAT_FONT         = new Font("Courier New", Font.PLAIN, 12);
    private static final Color CHAT_BG           = new Color(0, 0, 0, 140);

    private final GameClient client;
    private volatile ClientGameState state;
    private final Runnable onEscape;       // called when ESC pressed (go to main menu)
    private final Runnable onPlayAgain;    // called when "Back to Lobby" clicked
    private int animTick = 0;

    private int startCountdown = 3;
    private boolean countingDown = true;

    // ── Chat state ────────────────────────────────────────────────────────────
    private final Deque<Long>   chatTimestamps = new ArrayDeque<>();
    private final Deque<String> chatMessages   = new ArrayDeque<>();
    private boolean chatOpen = false;
    private final StringBuilder chatInput = new StringBuilder();

    // ── "Back to Lobby" overlay button ────────────────────────────────────────
    private JButton backToLobbyBtn;

    // ── Maze grid ─────────────────────────────────────────────────────────────
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

    public MultiplayerGamePanel(GameClient client, ClientGameState initialState,
                                Runnable onEscape, Runnable onPlayAgain) {
        this.client      = client;
        this.state       = initialState;
        this.onEscape    = onEscape;
        this.onPlayAgain = onPlayAgain;

        setLayout(null); // absolute layout so we can place the overlay button
        setPreferredSize(new Dimension(COLS * T, ROWS * T + 30));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // "Back to Lobby" button – hidden until game ends
        backToLobbyBtn = new JButton("Back to Lobby");
        backToLobbyBtn.setFont(new Font("Courier New", Font.BOLD, 16));
        backToLobbyBtn.setBackground(new Color(0, 150, 220));
        backToLobbyBtn.setForeground(Color.WHITE);
        backToLobbyBtn.setFocusPainted(false);
        backToLobbyBtn.setVisible(false);
        backToLobbyBtn.addActionListener(e -> onPlayAgain.run());
        // Positioned at centre; exact bounds set in paintComponent once we know size
        add(backToLobbyBtn);

        // Register chat callback
        client.setOnChat((sender, text) -> SwingUtilities.invokeLater(() -> {
            addChatMessage(sender + ": " + text);
            repaint();
        }));

        // 3-2-1 countdown overlay
        Timer cdTimer = new Timer(1000, null);
        cdTimer.addActionListener(e -> {
            startCountdown--;
            if (startCountdown <= 0) { countingDown = false; cdTimer.stop(); }
            repaint();
        });
        cdTimer.start();

        Timer repaintTimer = new Timer(GameConfig.TICK_MS, e -> { animTick++; repaint(); });
        repaintTimer.start();
    }

    public void updateState(ClientGameState newState) {
        this.state = newState;
        if (newState.gameResult != null) {
            SwingUtilities.invokeLater(() -> {
                // Show the "Back to Lobby" button centred on screen
                int bw = 200, bh = 44;
                int bx = (getWidth() - bw) / 2;
                int by = getHeight() / 2 + 75;
                backToLobbyBtn.setBounds(bx, by, bw, bh);
                backToLobbyBtn.setVisible(true);
                repaint();
            });
        }
    }

    // ── Chat helpers ──────────────────────────────────────────────────────────

    private void addChatMessage(String msg) {
        chatTimestamps.addLast(System.currentTimeMillis());
        chatMessages.addLast(msg);
        while (chatMessages.size() > CHAT_MAX_MESSAGES) {
            chatMessages.removeFirst();
            chatTimestamps.removeFirst();
        }
    }

    // ── Painting ──────────────────────────────────────────────────────────────

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
        drawChat(g2);

        if (countingDown)              drawCountdownOverlay(g2, startCountdown);
        else if (s.gameResult != null) drawResult(g2, s);

        if (chatOpen) drawChatInput(g2);

        // Keep button centred if window was resized
        if (backToLobbyBtn.isVisible()) {
            int bw = 200, bh = 44;
            backToLobbyBtn.setBounds((getWidth()-bw)/2, getHeight()/2+75, bw, bh);
        }
    }

    private void drawMaze(Graphics2D g2) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = c*T, y = r*T+30;
                if (GRID[r][c] == 1) {
                    g2.setColor(new Color(0,50,170));  g2.fillRect(x,y,T,T);
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
                g2.setColor(new Color(255,180,80,80));
                g2.fillOval(x+T/2-sz/2-4, y+T/2-sz/2-4, sz+8, sz+8);
            } else {
                g2.setColor(new Color(255,220,140)); g2.fillOval(x+17,y+17,6,6);
            }
        }
    }

    private void drawPlayers(Graphics2D g2, ClientGameState s) {
        int colorIdx = 0;
        for (ClientGameState.PlayerInfo p : s.players) {
            Color col = PLAYER_COLORS[colorIdx++ % PLAYER_COLORS.length];
            if (p.eliminated) {
                int x = p.col*T+2, y = p.row*T+2+30, sz = T-4;
                g2.setColor(new Color(100,0,0,120));  g2.fillOval(x,y,sz,sz);
                g2.setColor(new Color(200,0,0,180));  g2.setStroke(new BasicStroke(3));
                g2.drawLine(x+4,y+4,x+sz-4,y+sz-4);  g2.drawLine(x+sz-4,y+4,x+4,y+sz-4);
                g2.setStroke(new BasicStroke(1));
                g2.setFont(new Font("Courier New",Font.PLAIN,9));
                g2.setColor(new Color(200,100,100)); g2.drawString(p.name,x,y-2);
                continue;
            }
            int x = p.col*T+2, y = p.row*T+2+30, sz = T-4;
            if (p.role == util.Role.CHOMPER) {
                int mouth = (animTick%2==0) ? 40 : 10;
                double rot = 0;
                if (p.direction != null) switch (p.direction) {
                    case "LEFT": rot=180; break; case "UP": rot=270; break; case "DOWN": rot=90; break;
                }
                Graphics2D g3 = (Graphics2D)g2.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g3.translate(x+sz/2, y+sz/2); g3.rotate(Math.toRadians(rot));
                if (p.powered) {
                    int ga = 100 + (int)(80 * Math.sin(animTick * 0.3));
                    g3.setColor(new Color(255,120,0,ga)); g3.fillOval(-sz/2-6,-sz/2-6,sz+12,sz+12);
                }
                g3.setColor(col); g3.fillArc(-sz/2,-sz/2,sz,sz,(int)(mouth/2),(int)(360-mouth));
                g3.setColor(new Color(15,15,15)); g3.fillOval(-2,-sz/2+5,5,5); g3.dispose();
                g2.setFont(new Font("Courier New",Font.PLAIN,10)); g2.setColor(Color.WHITE); g2.drawString(p.name,x,y-2);
            } else {
                Color body = p.powered ? new Color(30,60,200) : col;
                g2.setColor(body); g2.fillArc(x,y,sz,sz,0,180); g2.fillRect(x,y+sz/2,sz,sz/2);
                g2.setColor(new Color(5,5,20));
                int segW = sz/3;
                for (int i=0;i<3;i++) g2.fillArc(x+i*segW,y+sz-5,segW,10,0,-180);
                if (!p.powered) {
                    g2.setColor(Color.WHITE); g2.fillOval(x+5,y+6,8,8); g2.fillOval(x+sz-13,y+6,8,8);
                    g2.setColor(new Color(0,0,200)); g2.fillOval(x+7,y+8,4,4); g2.fillOval(x+sz-11,y+8,4,4);
                } else {
                    g2.setColor(Color.WHITE); g2.fillOval(x+5,y+9,6,5); g2.fillOval(x+sz-11,y+9,6,5);
                    g2.drawArc(x+6,y+sz/2-2,5,5,0,-180); g2.drawArc(x+12,y+sz/2-2,5,5,0,180); g2.drawArc(x+18,y+sz/2-2,5,5,0,-180);
                }
                g2.setFont(new Font("Courier New",Font.PLAIN,10)); g2.setColor(Color.WHITE); g2.drawString(p.name,x,y-2);
            }
        }
    }

    private void drawHUD(Graphics2D g2, ClientGameState s) {
        g2.setColor(new Color(0,0,0,200)); g2.fillRect(0,0,getWidth(),30);
        int mins = s.timeLeft/60, secs = s.timeLeft%60;
        String timerStr = String.format("TIME %d:%02d",mins,secs);
        g2.setFont(new Font("Courier New",Font.BOLD,16));
        g2.setColor(s.timeLeft<30 ? Color.RED : Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(timerStr, getWidth()/2 - fm.stringWidth(timerStr)/2, 20);
        int x=5, colorIdx=0;
        g2.setFont(new Font("Courier New",Font.BOLD,12));
        for (ClientGameState.PlayerInfo p : s.players) {
            Color col = PLAYER_COLORS[colorIdx++ % PLAYER_COLORS.length];
            g2.setColor(p.eliminated ? new Color(150,60,60) : col);
            String info = p.name+":"+p.score;
            if (p.role==util.Role.CHOMPER) info += " \u2665"+p.lives;
            if (p.eliminated) info += " \u2718";
            g2.drawString(info,x,20);
            x += g2.getFontMetrics().stringWidth(info)+12;
        }
        if (!chatOpen) {
            g2.setFont(new Font("Courier New",Font.PLAIN,10)); g2.setColor(new Color(160,160,160));
            g2.drawString("T=chat  ESC=menu", getWidth()-110, getHeight()-4);
        }
    }

    // ── Chat log overlay ──────────────────────────────────────────────────────

    private void drawChat(Graphics2D g2) {
        if (chatMessages.isEmpty()) return;
        long now = System.currentTimeMillis();
        String[] msgs  = chatMessages.toArray(new String[0]);
        Long[]   times = chatTimestamps.toArray(new Long[0]);
        int count  = msgs.length;
        int panelH = count * CHAT_LINE_H + 8;
        int panelY = getHeight() - 30 - panelH;
        int panelX = 6, panelW = 290;
        g2.setColor(CHAT_BG); g2.fillRoundRect(panelX,panelY,panelW,panelH,8,8);
        g2.setFont(CHAT_FONT);
        FontMetrics fm = g2.getFontMetrics();
        for (int i=0; i<count; i++) {
            long age = now - times[i];
            float alpha = age > CHAT_FADE_MS ? 0f : Math.min(1f,(CHAT_FADE_MS - age)/1000f);
            if (alpha<=0) continue;
            String display = msgs[i];
            while (display.length()>2 && fm.stringWidth(display+"…") > panelW-10)
                display = display.substring(0,display.length()-1);
            if (!display.equals(msgs[i])) display += "…";
            g2.setColor(new Color(255,255,255,(int)(alpha*220)));
            g2.drawString(display, panelX+5, panelY + 6 + (i+1)*CHAT_LINE_H - 2);
        }
    }

    private void drawChatInput(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        int barH = 24, barY = h - barH;
        g2.setColor(new Color(0,0,0,210)); g2.fillRect(0,barY,w,barH);
        g2.setColor(new Color(80,140,255)); g2.drawRect(0,barY,w-1,barH-1);
        g2.setFont(new Font("Courier New",Font.PLAIN,13));
        g2.setColor(Color.CYAN);  g2.drawString("Say: ",6,barY+17);
        g2.setColor(Color.WHITE);
        String cursor = System.currentTimeMillis()%800<400 ? "|" : " ";
        g2.drawString(chatInput.toString()+cursor, 52, barY+17);
        g2.setFont(new Font("Courier New",Font.PLAIN,10)); g2.setColor(new Color(120,120,120));
        g2.drawString("ENTER=send  ESC=cancel", w-162, barY+17);
    }

    // ── Overlays ──────────────────────────────────────────────────────────────

    private void drawCountdownOverlay(Graphics2D g2, int count) {
        int w=getWidth(), h=getHeight();
        g2.setColor(new Color(0,0,0,160)); g2.fillRect(0,0,w,h);
        g2.setFont(new Font("Courier New",Font.BOLD,80));
        String txt = count>0 ? String.valueOf(count) : "GO!";
        g2.setColor(count>0 ? Color.CYAN : Color.YELLOW);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt,(w-fm.stringWidth(txt))/2,h/2+30);
        g2.setFont(new Font("Courier New",Font.BOLD,20)); g2.setColor(Color.WHITE);
        String sub="Get ready!"; fm=g2.getFontMetrics();
        g2.drawString(sub,(w-fm.stringWidth(sub))/2,h/2+80);
    }

    private void drawResult(Graphics2D g2, ClientGameState s) {
        int w=getWidth(), h=getHeight();
        g2.setColor(new Color(0,0,0,185)); g2.fillRect(0,0,w,h);
        g2.setFont(new Font("Courier New",Font.BOLD,36));
        boolean cw="CHOMPERS".equals(s.gameResult);
        String winner=cw?"CHOMPERS WIN!":"CHASERS WIN!";
        g2.setColor(cw?Color.YELLOW:new Color(255,80,80));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(winner,(w-fm.stringWidth(winner))/2,h/2-20);
        String reason=friendlyReason(s.resultReason);
        g2.setFont(new Font("Courier New",Font.PLAIN,18)); g2.setColor(Color.WHITE);
        fm=g2.getFontMetrics(); g2.drawString(reason,(w-fm.stringWidth(reason))/2,h/2+20);
        g2.setFont(new Font("Courier New",Font.PLAIN,13)); g2.setColor(Color.LIGHT_GRAY);
        String hint="ESC = main menu"; fm=g2.getFontMetrics();
        g2.drawString(hint,(w-fm.stringWidth(hint))/2,h/2+55);
    }

    private String friendlyReason(String raw) {
        if (raw==null) return "";
        switch (raw) {
            case "ALL_PELLETS":       return "Chomper collected all pellets!";
            case "ALL_CHASERS_EATEN": return "Chomper ate all the Chasers!";
            case "NO_LIVES":          return "Chomper lost all lives!";
            case "TIME_UP":           return "Time ran out!";
            case "CHOMPER_LEFT":      return "Chomper left the game!";
            default:                  return raw;
        }
    }

    // ── Key handling ──────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (chatOpen) {
            if (code == KeyEvent.VK_ENTER) {
                String msg = chatInput.toString().trim();
                if (!msg.isEmpty()) client.sendChat(msg);
                chatInput.setLength(0); chatOpen = false; repaint();
            } else if (code == KeyEvent.VK_ESCAPE) {
                chatInput.setLength(0); chatOpen = false; repaint();
            } else if (code == KeyEvent.VK_BACK_SPACE) {
                if (chatInput.length()>0) chatInput.deleteCharAt(chatInput.length()-1);
                repaint();
            }
            return;
        }
        switch (code) {
            case KeyEvent.VK_UP:    if (!countingDown) client.sendInput(Direction.UP);    break;
            case KeyEvent.VK_DOWN:  if (!countingDown) client.sendInput(Direction.DOWN);  break;
            case KeyEvent.VK_LEFT:  if (!countingDown) client.sendInput(Direction.LEFT);  break;
            case KeyEvent.VK_RIGHT: if (!countingDown) client.sendInput(Direction.RIGHT); break;
            case KeyEvent.VK_T:     chatOpen=true; repaint(); break;
            case KeyEvent.VK_ESCAPE:
                if (onEscape != null) onEscape.run();
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (!chatOpen) return;
        char c = e.getKeyChar();
        if (c >= 32 && c < 127 && chatInput.length() < 100) { chatInput.append(c); repaint(); }
    }

    @Override public void keyReleased(KeyEvent e) {}
}