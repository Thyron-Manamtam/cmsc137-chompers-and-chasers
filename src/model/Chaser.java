package model;

import util.Direction;
import util.GameConfig;

import java.util.*;

public class Chaser extends Entity {

    public enum Mode { CHASE, FRIGHTENED, EATEN }

    private Mode mode;
    private int  frightenedTicks;
    private final Random random;
    private final int moveDelay;
    private int delayCounter;

    // M2: network identity — if playerName is non-null, this is a human chaser
    private String playerName;
    private int    playerId;

    public Chaser(int startRow, int startCol, int moveDelay) {
        super(startRow, startCol);
        this.mode            = Mode.CHASE;
        this.frightenedTicks = 0;
        this.random          = new Random();
        this.moveDelay       = moveDelay;
        this.delayCounter    = 0;
        this.playerName      = null;
        this.playerId        = -1;
    }

    public void frighten() {
        if (mode != Mode.EATEN) {
            mode            = Mode.FRIGHTENED;
            frightenedTicks = GameConfig.POWER_DURATION;
        }
    }

    public void tickFrightened() {
        if (mode == Mode.FRIGHTENED) {
            frightenedTicks--;
            if (frightenedTicks <= 0) mode = Mode.CHASE;
        }
    }

    public void eat() { mode = Mode.EATEN; }

    @Override
    public void respawn() {
        super.respawn();
        mode            = Mode.CHASE;
        frightenedTicks = 0;
        delayCounter    = 0;
    }

    public void moveToward(Player target, Maze maze) {
        delayCounter++;
        if (delayCounter < moveDelay) return;
        delayCounter = 0;

        switch (mode) {
            case CHASE:      moveViaBFS(target.getRow(), target.getCol(), maze); break;
            case FRIGHTENED: moveRandom(maze); break;
            case EATEN:      break;
        }
    }

    @Override
    public void move(Maze maze) { /* no-op for AI; use moveToward */ }

    private void moveViaBFS(int targetRow, int targetCol, Maze maze) {
        Direction dir = bfsDirection(targetRow, targetCol, maze);
        if (dir != Direction.NONE) {
            row += dir.getDRow();
            col += dir.getDCol();
        }
    }

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

    private Direction bfsDirection(int targetRow, int targetCol, Maze maze) {
        if (row == targetRow && col == targetCol) return Direction.NONE;
        int rows = maze.getRows(), cols = maze.getCols();
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue  = new LinkedList<>();
        visited[row][col]   = true;
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

    public boolean catches(Player player) {
        return mode != Mode.EATEN && collidesWith(player);
    }

    // ── Network setters ──────────────────────────────────────
    public void setRow(int r)            { this.row = r; }
    public void setCol(int c)            { this.col = c; }
    public void setMode(Mode m)          { this.mode = m; }
    public void setFrightenedTicks(int t){ this.frightenedTicks = t; }
    public void setPlayerName(String n)  { this.playerName = n; }
    public void setPlayerId(int id)      { this.playerId = id; }

    // ── Getters ──────────────────────────────────────────────
    public Mode   getMode()            { return mode; }
    public boolean isFrightened()      { return mode == Mode.FRIGHTENED; }
    public boolean isEaten()           { return mode == Mode.EATEN; }
    public int    getFrightenedTicks() { return frightenedTicks; }
    public String getPlayerName()      { return playerName; }
    public int    getPlayerId()        { return playerId; }
    public boolean isHuman()           { return playerName != null; }
}