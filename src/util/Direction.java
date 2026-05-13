package util;

public enum Direction {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1),
    NONE(0, 0);

    private final int dRow, dCol;

    Direction(int dRow, int dCol) {
        this.dRow = dRow;
        this.dCol = dCol;
    }

    public int getDRow() { return dRow; }
    public int getDCol() { return dCol; }
    public int[] toDelta() { return new int[]{ dRow, dCol }; }

    public Direction opposite() {
        switch (this) {
            case UP:    return DOWN;
            case DOWN:  return UP;
            case LEFT:  return RIGHT;
            case RIGHT: return LEFT;
            default:    return NONE;
        }
    }
}