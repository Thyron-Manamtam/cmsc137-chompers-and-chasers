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

/**
 * Central game controller for Milestone 1 (single-player).
 *
 * Responsibilities:
 *   - Owns and updates all model objects (Maze, Player, Chasers)
 *   - Drives the game loop via a Swing Timer
 *   - Implements the GameState machine
 *   - Exposes read-only state to the view
 *
 * Milestone 2 note: This controller will be split into:
 *   - GameServer  — authoritative logic (runs server-side)
 *   - GameClient  — thin client that sends input & renders server state
 * The model classes (Maze, Player, Chaser) are already designed to be
 * serializable / reconstructible for network transfer.
 */
public class GameController {

    // ── Model ────────────────────────────────────────────────
    private Maze         maze;
    private Player       player;
    private List<Chaser> chasers;

    // ── State ────────────────────────────────────────────────
    private GameState state;
    private int       deathAnimTicks;
    private int       eatMultiplier; // doubles each chaser eaten in one power

    // ── Infrastructure ───────────────────────────────────────
    private final GamePanel panel;
    private final Timer     gameTimer;

    public GameController(GamePanel panel) {
        this.panel     = panel;
        this.gameTimer = new Timer(GameConfig.TICK_MS, e -> tick());
        initGame();
    }

    // ── Initialisation ───────────────────────────────────────

    private void initGame() {
        maze   = new Maze();
        // Milestone 1: player is always a Chomper
        player = new Player(7, 7, Role.CHOMPER);

        // Four AI chasers at maze corners with staggered speeds
        chasers = new ArrayList<>();
        chasers.add(new Chaser(1,  1,  1));
        chasers.add(new Chaser(1,  13, 2));
        chasers.add(new Chaser(13, 1,  2));
        chasers.add(new Chaser(13, 13, 1));

        state        = GameState.START;
        eatMultiplier = 1;
    }

    // ── Game Loop ────────────────────────────────────────────

    private void tick() {
        switch (state) {
            case PLAYING: tickPlaying(); break;
            case DEAD:    tickDead();    break;
            default:      break;
        }
        panel.repaint();
    }

    private void tickPlaying() {
        player.move(maze);

        // Propagate power state to all chasers
        if (player.isPowered()) {
            for (Chaser c : chasers) { if (!c.isEaten()) c.frighten(); }
        } else {
            for (Chaser c : chasers) c.tickFrightened();
        }

        // Move each chaser and check collisions
        for (Chaser c : chasers) {
            c.moveToward(player, maze);

            if (c.catches(player)) {
                if (player.isPowered()) {
                    // Chomper eats the chaser
                    c.eat();
                    player.addScore(GameConfig.SCORE_EAT_BASE * eatMultiplier);
                    eatMultiplier *= 2;
                } else {
                    handlePlayerDeath();
                    return;
                }
            }
        }

        // Win: all pellets collected
        if (maze.countRemainingPellets() == 0) {
            state = GameState.WIN;
        }
    }

    private void handlePlayerDeath() {
        player.loseLife();
        if (!player.isAlive()) {
            state = GameState.GAME_OVER;
        } else {
            state         = GameState.DEAD;
            deathAnimTicks = GameConfig.DEATH_TICKS;
            for (Chaser c : chasers) c.respawn();
        }
    }

    private void tickDead() {
        if (--deathAnimTicks <= 0) {
            state = GameState.PLAYING;
        }
    }

    // ── Public Commands ──────────────────────────────────────

    /** Start a fresh game from the title screen or after game over / win. */
    public void startGame() {
        initGame();
        state = GameState.PLAYING;
        if (!gameTimer.isRunning()) gameTimer.start();
    }

    /** Restart mid-game (also callable from game-over / win screens). */
    public void restartGame() {
        initGame();
        state = GameState.PLAYING;
        if (!gameTimer.isRunning()) gameTimer.start();
        else panel.repaint();
    }

    /** Toggle pause (no-op unless PLAYING or PAUSED). */
    public void togglePause() {
        if      (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED)  state = GameState.PLAYING;
        panel.repaint();
    }

    /** Feed a direction command from the player's keyboard input. */
    public void setPlayerDirection(Direction d) {
        if (state == GameState.PLAYING) player.setCurrentDirection(d);
    }

    /** Kick off the Swing timer (called once after construction). */
    public void start() { gameTimer.start(); }

    // ── Read-only Accessors (view only reads, never writes) ──

    public Maze         getMaze()          { return maze; }
    public Player       getPlayer()        { return player; }
    public List<Chaser> getChasers()       { return chasers; }
    public GameState    getState()         { return state; }
    public int          getDeathAnimTicks(){ return deathAnimTicks; }
}
