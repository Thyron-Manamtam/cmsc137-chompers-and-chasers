package model;

public class Pellet {
    public enum Type { NORMAL, POWER }

    private final int row, col;
    private final Type type;
    private boolean collected;

    public Pellet(int row, int col, Type type) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.collected = false;
    }

    public void collect()        { this.collected = true; }
    public void reset()          { this.collected = false; }
    public boolean isCollected() { return collected; }
    public boolean isPower()     { return type == Type.POWER; }
    public int getRow()          { return row; }
    public int getCol()          { return col; }
    public Type getType()        { return type; }
}