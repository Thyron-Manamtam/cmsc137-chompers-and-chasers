package model;

import util.Direction;
import util.GameConfig;
import util.Role;

public class Player extends Entity {

    private Role role;
    private int score;
    private int lives;
    private Direction currentDirection;
    private Direction bufferedDirection;
    private int mouthFrame;
    private boolean powered;
    private int powerTicks;

    // M2: multiplayer identity
    private String playerName;
    private int    playerId;   // assigned by server (0-3)

    public Player(int startRow, int startCol, Role role) {
        super(startRow, startCol);
        this.role             = role;
        this.score            = 0;
        this.lives            = GameConfig.MAX_LIVES;
        this.currentDirection = Direction.NONE;
        this.bufferedDirection= Direction.NONE;
        this.mouthFrame       = 0;
        this.powered          = false;
        this.powerTicks       = 0;
        this.playerName       = "Player";
        this.playerId         = 0;
    }

    @Override
    public void move(Maze maze) {
        if (bufferedDirection != Direction.NONE) {
            int[] bd = bufferedDirection.toDelta();
            if (!maze.isWall(row + bd[0], col + bd[1])) {
                currentDirection  = bufferedDirection;
                bufferedDirection = Direction.NONE;
            }
        }

        int[] delta = currentDirection.toDelta();
        int newRow  = row + delta[0];
        int newCol  = col + delta[1];

        if (!maze.isWall(newRow, newCol)) {
            row = newRow;
            col = newCol;
            mouthFrame = (mouthFrame + 1) % 4;

            if (role == Role.CHOMPER) {
                Pellet collected = maze.collectPelletAt(row, col);
                if (collected != null) {
                    score += collected.isPower() ? GameConfig.SCORE_POWER : GameConfig.SCORE_PELLET;
                    if (collected.isPower()) {
                        powered    = true;
                        powerTicks = GameConfig.POWER_DURATION;
                    }
                }
            }
        }

        if (powered) {
            powerTicks--;
            if (powerTicks <= 0) powered = false;
        }
    }

    public void loseLife() {
        lives--;
        respawn();
        currentDirection  = Direction.NONE;
        bufferedDirection = Direction.NONE;
        powered    = false;
        powerTicks = 0;
    }

    public void requestDirection(Direction d) {
        bufferedDirection = d;
        currentDirection  = d;
    }

    public void setCurrentDirection(Direction d) { requestDirection(d); }

    // ── Setters for network snapshots ────────────────────────
    public void setRow(int r)            { this.row = r; }
    public void setCol(int c)            { this.col = c; }
    public void setScore(int s)          { this.score = s; }
    public void setLives(int l)          { this.lives = l; }
    public void setPowered(boolean p)    { this.powered = p; }
    public void setPowerTicks(int t)     { this.powerTicks = t; }
    public void setMouthFrame(int f)     { this.mouthFrame = f; }
    public void setDirection(Direction d){ this.currentDirection = d; }

    // ── Getters ──────────────────────────────────────────────
    public Role      getRole()        { return role; }
    public int       getScore()       { return score; }
    public int       getLives()       { return lives; }
    public boolean   isPowered()      { return powered; }
    public int       getPowerTicks()  { return powerTicks; }
    public Direction getDirection()   { return currentDirection; }
    public int       getMouthFrame()  { return mouthFrame; }
    public boolean   isAlive()        { return lives > 0; }
    public void      addScore(int n)  { score += n; }
    public String    getPlayerName()  { return playerName; }
    public int       getPlayerId()    { return playerId; }
    public void      setPlayerName(String n) { this.playerName = n; }
    public void      setPlayerId(int id)     { this.playerId = id; }
}