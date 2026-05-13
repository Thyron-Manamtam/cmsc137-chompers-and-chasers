package controller;

import model.*;
import util.*;
import view.GamePanel;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

/**
 * Single-player game controller (Milestone 1 logic, unchanged).
 * Used only when AppMode == SINGLE_PLAYER.
 */
public class GameController {

    private Maze         maze;
    private Player       player;
    private List<Chaser> chasers;

    private GameState state;
    private int       deathAnimTicks;
    private int       eatMultiplier;

    private final GamePanel panel;
    private final Timer     gameTimer;

    public GameController(GamePanel panel) {
        this.panel     = panel;
        this.gameTimer = new Timer(GameConfig.TICK_MS, e -> tick());
        initGame();
    }

    private void initGame() {
        maze   = new Maze();
        player = new Player(7, 7, Role.CHOMPER);
        player.setPlayerName("You");

        chasers = new ArrayList<>();
        chasers.add(new Chaser(1,  1,  1));
        chasers.add(new Chaser(1,  13, 2));
        chasers.add(new Chaser(13, 1,  2));
        chasers.add(new Chaser(13, 13, 1));

        state         = GameState.START;
        eatMultiplier = 1;
    }

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

        if (maze.countRemainingPellets() == 0) {
            state = GameState.WIN;
        }
    }

    private void handlePlayerDeath() {
        player.loseLife();
        if (!player.isAlive()) {
            state = GameState.GAME_OVER;
        } else {
            state          = GameState.DEAD;
            deathAnimTicks = GameConfig.DEATH_TICKS;
            for (Chaser c : chasers) c.respawn();
            eatMultiplier  = 1;
        }
    }

    private void tickDead() {
        if (--deathAnimTicks <= 0) state = GameState.PLAYING;
    }

    // ── Public commands ──────────────────────────────────────

    public void startGame() {
        initGame();
        state = GameState.PLAYING;
        if (!gameTimer.isRunning()) gameTimer.start();
    }

    public void restartGame() {
        initGame();
        state = GameState.PLAYING;
        if (!gameTimer.isRunning()) gameTimer.start();
        else panel.repaint();
    }

    public void togglePause() {
        if      (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED)  state = GameState.PLAYING;
        panel.repaint();
    }

    public void setPlayerDirection(Direction d) {
        if (state == GameState.PLAYING) player.setCurrentDirection(d);
    }

    public void start() { gameTimer.start(); }

    // ── Accessors ────────────────────────────────────────────

    public Maze         getMaze()          { return maze; }
    public Player       getPlayer()        { return player; }
    public List<Chaser> getChasers()       { return chasers; }
    public GameState    getState()         { return state; }
    public int          getDeathAnimTicks(){ return deathAnimTicks; }
}