package model;

/**
 * Base class for all positioned game entities.
 * Encapsulates grid coordinates and spawn position for respawning.
 */
public abstract class Entity implements Movable {
    protected int row, col;
    protected final int spawnRow, spawnCol;

    protected Entity(int startRow, int startCol) {
        this.row = startRow;
        this.col = startCol;
        this.spawnRow = startRow;
        this.spawnCol = startCol;
    }

    /** Reset this entity to its original spawn position. */
    public void respawn() {
        this.row = spawnRow;
        this.col = spawnCol;
    }

    public boolean isAt(int r, int c) {
        return row == r && col == c;
    }

    public boolean collidesWith(Entity other) {
        return row == other.row && col == other.col;
    }

    @Override public int getRow() { return row; }
    @Override public int getCol() { return col; }
}
