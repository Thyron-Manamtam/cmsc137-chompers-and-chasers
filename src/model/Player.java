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

    // Pixel-level smooth rendering fields
    private float pixelX, pixelY;
    private float targetPixelX, targetPixelY;

    public Player(int startRow, int startCol, Role role) {
        super(startRow, startCol);
        this.role = role;
        this.score = 0;
        this.lives = GameConfig.MAX_LIVES;
        this.currentDirection  = Direction.NONE;
        this.bufferedDirection = Direction.NONE;
        this.mouthFrame = 0;
        this.powered    = false;
        this.powerTicks = 0;
        this.pixelX = startCol * GameConfig.TILE_SIZE;
        this.pixelY = startRow * GameConfig.TILE_SIZE;
        this.targetPixelX = pixelX;
        this.targetPixelY = pixelY;
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
            row = newRow; col = newCol;
            mouthFrame = (mouthFrame + 1) % 4;
            targetPixelX = col * GameConfig.TILE_SIZE;
            targetPixelY = row * GameConfig.TILE_SIZE;

            if (role == Role.CHOMPER) {
                Pellet collected = maze.collectPelletAt(row, col);
                if (collected != null) {
                    score += collected.isPower() ? GameConfig.SCORE_POWER : GameConfig.SCORE_PELLET;
                    if (collected.isPower()) { powered = true; powerTicks = GameConfig.POWER_DURATION; }
                }
            }
        }

        if (powered) { powerTicks--; if (powerTicks <= 0) powered = false; }
    }

    /** Smoothly moves pixel position toward grid target. Call every repaint. */
    public void interpolatePixel(float speed) {
        float dx = targetPixelX - pixelX;
        float dy = targetPixelY - pixelY;
        if (Math.abs(dx) < speed) pixelX = targetPixelX; else pixelX += Math.signum(dx) * speed;
        if (Math.abs(dy) < speed) pixelY = targetPixelY; else pixelY += Math.signum(dy) * speed;
    }

    public float getPixelX() { return pixelX; }
    public float getPixelY() { return pixelY; }

    public void loseLife() {
        lives--;
        respawn();
        currentDirection  = Direction.NONE;
        bufferedDirection = Direction.NONE;
        powered    = false;
        powerTicks = 0;
    }

    @Override
    public void respawn() {
        super.respawn();
        pixelX = spawnCol * GameConfig.TILE_SIZE;
        pixelY = spawnRow * GameConfig.TILE_SIZE;
        targetPixelX = pixelX;
        targetPixelY = pixelY;
    }

    public void requestDirection(Direction d) { bufferedDirection = d; currentDirection = d; }
    public void setCurrentDirection(Direction d) { requestDirection(d); }

    public Role      getRole()       { return role; }
    public int       getScore()      { return score; }
    public int       getLives()      { return lives; }
    public int       getMouthFrame() { return mouthFrame; }
    public boolean   isPowered()     { return powered; }
    public int       getPowerTicks() { return powerTicks; }
    public Direction getDirection()  { return currentDirection; }
    public boolean   isAlive()       { return lives > 0; }
    public void      addScore(int s) { score += s; }
}