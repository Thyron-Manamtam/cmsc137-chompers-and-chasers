package model;

import util.Direction;
import util.GameConfig;

import java.util.*;

public class Chaser extends Entity {

    public enum Mode { CHASE, FRIGHTENED, EATEN }

    private Mode mode;
    private int  frightenedTicks;
    private final Random random;
    // moveDelay=1 → moves every tick; smoother because TICK_MS is now 150 ms
    private final int moveDelay;
    private int delayCounter;

    // Pixel-level interpolation fields (used by renderer only)
    private float pixelX, pixelY;   // current rendered pixel position
    private float targetPixelX, targetPixelY;

    public Chaser(int startRow, int startCol, int moveDelay) {
        super(startRow, startCol);
        this.mode            = Mode.CHASE;
        this.frightenedTicks = 0;
        this.random          = new Random();
        this.moveDelay       = moveDelay;
        this.delayCounter    = 0;
        // Initialise pixel position to grid position
        this.pixelX = startCol * GameConfig.TILE_SIZE;
        this.pixelY = startRow * GameConfig.TILE_SIZE;
        this.targetPixelX = pixelX;
        this.targetPixelY = pixelY;
    }

    public void frighten() {
        if (mode != Mode.EATEN) { mode = Mode.FRIGHTENED; frightenedTicks = GameConfig.POWER_DURATION; }
    }

    public void tickFrightened() {
        if (mode == Mode.FRIGHTENED) { frightenedTicks--; if (frightenedTicks <= 0) mode = Mode.CHASE; }
    }

    public void eat() { mode = Mode.EATEN; }

    @Override
    public void respawn() {
        super.respawn();
        mode = Mode.CHASE; frightenedTicks = 0; delayCounter = 0;
        pixelX = spawnCol * GameConfig.TILE_SIZE;
        pixelY = spawnRow * GameConfig.TILE_SIZE;
        targetPixelX = pixelX;
        targetPixelY = pixelY;
    }

    public void moveToward(Player target, Maze maze) {
        delayCounter++;
        if (delayCounter < moveDelay) return;
        delayCounter = 0;
        int prevRow = row, prevCol = col;
        switch (mode) {
            case CHASE:      moveViaBFS(target.getRow(), target.getCol(), maze); break;
            case FRIGHTENED: moveRandom(maze); break;
            case EATEN:      break;
        }
        // Update pixel target when grid position changes
        if (row != prevRow || col != prevCol) {
            targetPixelX = col * GameConfig.TILE_SIZE;
            targetPixelY = row * GameConfig.TILE_SIZE;
        }
    }

    /**
     * Smoothly interpolates the rendered pixel position toward the grid target.
     * Call this every repaint cycle. speed controls pixels moved per call.
     */
    public void interpolatePixel(float speed) {
        float dx = targetPixelX - pixelX;
        float dy = targetPixelY - pixelY;
        if (Math.abs(dx) < speed) pixelX = targetPixelX; else pixelX += Math.signum(dx) * speed;
        if (Math.abs(dy) < speed) pixelY = targetPixelY; else pixelY += Math.signum(dy) * speed;
    }

    public float getPixelX() { return pixelX; }
    public float getPixelY() { return pixelY; }

    @Override public void move(Maze maze) {}

    private void moveViaBFS(int targetRow, int targetCol, Maze maze) {
        Direction dir = bfsDirection(targetRow, targetCol, maze);
        if (dir != Direction.NONE) { row += dir.getDRow(); col += dir.getDCol(); }
    }

    private void moveRandom(Maze maze) {
        Direction[] dirs = Direction.values();
        List<Direction> valid = new ArrayList<>();
        for (Direction d : dirs)
            if (d != Direction.NONE && !maze.isWall(row + d.getDRow(), col + d.getDCol()))
                valid.add(d);
        if (!valid.isEmpty()) {
            Direction chosen = valid.get(random.nextInt(valid.size()));
            row += chosen.getDRow(); col += chosen.getDCol();
        }
    }

    private Direction bfsDirection(int targetRow, int targetCol, Maze maze) {
        if (row == targetRow && col == targetCol) return Direction.NONE;
        int rows = maze.getRows(), cols = maze.getCols();
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue  = new LinkedList<>();
        visited[row][col] = true;
        for (Direction d : Direction.values()) {
            if (d == Direction.NONE) continue;
            int nr = row + d.getDRow(), nc = col + d.getDCol();
            if (!maze.isWall(nr, nc) && !visited[nr][nc]) {
                visited[nr][nc] = true;
                queue.offer(new int[]{ nr, nc, d.ordinal() });
            }
        }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1], dirOrd = cur[2];
            if (r == targetRow && c == targetCol) return Direction.values()[dirOrd];
            for (Direction d : Direction.values()) {
                if (d == Direction.NONE) continue;
                int nr = r + d.getDRow(), nc = c + d.getDCol();
                if (!maze.isWall(nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{ nr, nc, dirOrd });
                }
            }
        }
        return Direction.NONE;
    }

    public boolean catches(Player player) { return mode != Mode.EATEN && collidesWith(player); }

    public Mode    getMode()           { return mode; }
    public boolean isFrightened()      { return mode == Mode.FRIGHTENED; }
    public boolean isEaten()           { return mode == Mode.EATEN; }
    public int     getFrightenedTicks(){ return frightenedTicks; }
}