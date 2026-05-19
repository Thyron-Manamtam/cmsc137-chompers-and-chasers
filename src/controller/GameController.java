package controller;

import model.*;
import util.Direction;
import util.GameConfig;
import util.GameState;
import util.Role;
import view.GamePanel;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class GameController {

    private Maze         maze;
    private Player       player;
    private List<Chaser> chasers;

    private GameState state;
    private int       deathAnimTicks;
    private int       eatMultiplier;

    // ── Game timer ─────────────────────────────────────────────────────────────
    // Tracks elapsed ticks since game started to compute elapsed seconds
    private int  elapsedTicks    = 0;
    private int  lastElapsedSecs = 0;

    // Announcement message shown on screen (e.g. "SUPER PELLETS MOVING IN 3…")
    private String announcement     = null;
    private int    announcementTicks = 0;
    private static final int ANNOUNCE_DISPLAY_TICKS = 20; // ~3 s at 150 ms/tick

    // Relocation checkpoints in elapsed seconds: 30, 60, 90
    private static final int[] RELOCATE_AT_ELAPSED = { 30, 60, 90 };
    private boolean[] relocateDone = new boolean[3];
    private boolean[] warnDone     = new boolean[3];

    private final GamePanel panel;
    private final Timer     gameTimer;
    private final Timer     renderTimer;

    public GameController(GamePanel panel) {
        this.panel     = panel;
        this.gameTimer = new Timer(GameConfig.TICK_MS, e -> tick());
        // Render timer: ~60 fps for smooth interpolation between game ticks
        this.renderTimer = new Timer(16, e -> panel.repaint()); // ~16ms ≈ 60fps
        initGame();
    }

    private void initGame() {
        maze   = new Maze();
        player = new Player(7, 7, Role.CHOMPER);

        chasers = new ArrayList<>();
        // moveDelay=1 → every tick (smooth); variation achieved by the BFS being slower
        chasers.add(new Chaser(1,  1,  1));
        chasers.add(new Chaser(1,  13, 2));
        chasers.add(new Chaser(13, 1,  2));
        chasers.add(new Chaser(13, 13, 1));

        state           = GameState.START;
        eatMultiplier   = 1;
        elapsedTicks    = 0;
        lastElapsedSecs = 0;
        relocateDone    = new boolean[3];
        warnDone        = new boolean[3];
        announcement     = null;
        announcementTicks = 0;
    }

    private void tick() {
        switch (state) {
            case PLAYING: tickPlaying(); break;
            case DEAD:    tickDead();    break;
            default:      break;
        }
        // Don't call repaint() here anymore; renderTimer handles it at ~60 fps
    }

    private void tickPlaying() {
        elapsedTicks++;
        int elapsedSecs = (elapsedTicks * GameConfig.TICK_MS) / 1000;

        // Decrement announcement display counter
        if (announcementTicks > 0) announcementTicks--;
        else announcement = null;

        // Check super-pellet warn / relocate schedule
        for (int i = 0; i < RELOCATE_AT_ELAPSED.length; i++) {
            int target = RELOCATE_AT_ELAPSED[i];
            int warnAt = target - GameConfig.SUPER_PELLET_WARN_BEFORE_S;

            if (!warnDone[i] && elapsedSecs >= warnAt) {
                warnDone[i] = true;
                showAnnouncement("⚠ SUPER PELLETS MOVING IN 3…");
            }
            if (!relocateDone[i] && elapsedSecs >= target) {
                relocateDone[i] = true;
                maze.relocatePowerPellets();
                showAnnouncement("✦ SUPER PELLETS RELOCATED!");
            }
        }

        player.move(maze);

        if (player.isPowered()) {
            for (Chaser c : chasers) { if (!c.isEaten()) c.frighten(); }
        } else {
            for (Chaser c : chasers) c.tickFrightened();
        }

        for (Chaser c : chasers) {
            c.moveToward(player, maze);
            if (c.catches(player)) {
                if (player.isPowered()) {
                    c.eat();
                    player.addScore(GameConfig.SCORE_EAT_BASE * eatMultiplier);
                    eatMultiplier *= 2;
                } else {
                    handlePlayerDeath();
                    return;
                }
            }
        }

        if (maze.countRemainingPellets() == 0) state = GameState.WIN;
    }

    private void showAnnouncement(String msg) {
        announcement      = msg;
        announcementTicks = ANNOUNCE_DISPLAY_TICKS;
    }

    private void handlePlayerDeath() {
        player.loseLife();
        if (!player.isAlive()) {
            state = GameState.GAME_OVER;
        } else {
            state          = GameState.DEAD;
            deathAnimTicks = GameConfig.DEATH_TICKS;
            for (Chaser c : chasers) c.respawn();
        }
    }

    private void tickDead() { if (--deathAnimTicks <= 0) state = GameState.PLAYING; }

    public void startGame() {
        initGame(); state = GameState.PLAYING;
        if (!gameTimer.isRunning()) gameTimer.start();
        if (!renderTimer.isRunning()) renderTimer.start();
    }

    public void restartGame() {
        initGame(); state = GameState.PLAYING;
        if (!gameTimer.isRunning()) gameTimer.start();
        if (!renderTimer.isRunning()) renderTimer.start();
        else panel.repaint();
    }

    public void togglePause() {
        if      (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED)  state = GameState.PLAYING;
        panel.repaint();
    }

    public void setPlayerDirection(Direction d) { if (state == GameState.PLAYING) player.setCurrentDirection(d); }

    public void start() { 
        gameTimer.start(); 
        renderTimer.start(); 
    }

    public Maze         getMaze()             { return maze; }
    public Player       getPlayer()           { return player; }
    public List<Chaser> getChasers()          { return chasers; }
    public GameState    getState()            { return state; }
    public int          getDeathAnimTicks()   { return deathAnimTicks; }
    public String       getAnnouncement()     { return announcement; }
    public int          getAnnouncementTicks(){ return announcementTicks; }
}