package model;

import util.Direction;
import util.GameConfig;

import java.util.*;

/**
 * AI-controlled Chaser for Milestone 1.
 * Uses BFS pathfinding to chase the Chomper, and moves randomly when frightened.
 *
 * Milestone 2: this class will be replaced / extended by a network-controlled
 * HumanChaser that reads moves from a remote client instead of running BFS.
 * The AI logic here is explicitly temporary.
 */
public class Chaser extends Entity {

    public enum Mode { CHASE, FRIGHTENED, EATEN }

    private Mode mode;
    private int  frightenedTicks;
    private final Random random;

    /** Ticks to skip between moves — lower = faster. */
    private final int moveDelay;
    private int delayCounter;

    public Chaser(int startRow, int startCol, int moveDelay) {
        super(startRow, startCol);
        this.mode           = Mode.CHASE;
        this.frightenedTicks = 0;
        this.random         = new Random();
        this.moveDelay      = moveDelay;
        this.delayCounter   = 0;
    }

    // ── State transitions ────────────────────────────────────

    /** Called when the Chomper collects a power pellet. */
    public void frighten() {
        if (mode != Mode.EATEN) {
            mode            = Mode.FRIGHTENED;
            frightenedTicks = GameConfig.POWER_DURATION;
        }
    }

    /** Tick down frightened timer; reverts to CHASE when expired. */
    public void tickFrightened() {
        if (mode == Mode.FRIGHTENED) {
            frightenedTicks--;
            if (frightenedTicks <= 0) mode = Mode.CHASE;
        }
    }

    /** Mark this chaser as eaten (temporarily removed from play). */
    public void eat() { mode = Mode.EATEN; }

    @Override
    public void respawn() {
        super.respawn();
        mode            = Mode.CHASE;
        frightenedTicks = 0;
        delayCounter    = 0;
    }

    // ── Movement ─────────────────────────────────────────────

    /**
     * Primary movement method called each tick.
     * Handles throttling, mode routing, and pathfinding.
     */
    public void moveToward(Player target, Maze maze) {
        delayCounter++;
        if (delayCounter < moveDelay) return;
        delayCounter = 0;

        switch (mode) {
            case CHASE:      moveViaBFS(target.getRow(), target.getCol(), maze); break;
            case FRIGHTENED: moveRandom(maze); break;
            case EATEN:      /* stationary until respawn */                      break;
        }
    }

    @Override
    public void move(Maze maze) {
        // No-op: Chasers require a target; use moveToward(Player, Maze) instead.
    }

    /** BFS-guided step toward (targetRow, targetCol). */
    private void moveViaBFS(int targetRow, int targetCol, Maze maze) {
        Direction dir = bfsDirection(targetRow, targetCol, maze);
        if (dir != Direction.NONE) {
            row += dir.getDRow();
            col += dir.getDCol();
        }
    }

    /** Random valid step (used in frightened mode). */
    private void moveRandom(Maze maze) {
        Direction[] dirs = Direction.values();
        List<Direction> valid = new ArrayList<>();
        for (Direction d : dirs) {
            if (d != Direction.NONE && !maze.isWall(row + d.getDRow(), col + d.getDCol()))
                valid.add(d);
        }
        if (!valid.isEmpty()) {
            Direction chosen = valid.get(random.nextInt(valid.size()));
            row += chosen.getDRow();
            col += chosen.getDCol();
        }
    }

    /**
     * BFS from current position to target.
     * Returns the first Direction to step in, or NONE if unreachable.
     */
    private Direction bfsDirection(int targetRow, int targetCol, Maze maze) {
        if (row == targetRow && col == targetCol) return Direction.NONE;

        int rows = maze.getRows(), cols = maze.getCols();
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue  = new LinkedList<>(); // [r, c, firstDirOrdinal]

        visited[row][col] = true;
        for (Direction d : Direction.values()) {
            if (d == Direction.NONE) continue;
            int nr = row + d.getDRow();
            int nc = col + d.getDCol();
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
                int nr = r + d.getDRow();
                int nc = c + d.getDCol();
                if (!maze.isWall(nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{ nr, nc, dirOrd });
                }
            }
        }
        return Direction.NONE;
    }

    // ── Collision ────────────────────────────────────────────

    /** Returns true if this chaser is in a position to catch the player. */
    public boolean catches(Player player) {
        return mode != Mode.EATEN && collidesWith(player);
    }

    // ── Getters ──────────────────────────────────────────────

    public Mode getMode()            { return mode; }
    public boolean isFrightened()    { return mode == Mode.FRIGHTENED; }
    public boolean isEaten()         { return mode == Mode.EATEN; }
    public int getFrightenedTicks()  { return frightenedTicks; }
}
