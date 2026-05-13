package model;

import java.util.ArrayList;
import java.util.List;

public class Maze {
    public static final int WALL = 1;
    public static final int PATH = 0;

    private static final int[][] POWER_POSITIONS = {
        {1, 1}, {1, 13}, {13, 1}, {13, 13}
    };

    private final int[][] grid;
    private List<Pellet> pellets;
    private final int rows, cols;

    public Maze() {
        grid = new int[][] {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,1,0,1},
            {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
            {1,0,0,0,0,0,1,0,1,0,0,0,0,0,1},
            {1,0,1,1,1,0,1,0,1,0,1,1,1,0,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,1,0,1},
            {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
            {1,0,0,0,0,0,1,0,1,0,0,0,0,0,1},
            {1,0,1,1,1,0,0,0,0,0,1,1,1,0,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        };
        rows = grid.length;
        cols = grid[0].length;
        buildPellets();
    }

    private boolean isPowerPosition(int r, int c) {
        for (int[] pos : POWER_POSITIONS) {
            if (pos[0] == r && pos[1] == c) return true;
        }
        return false;
    }

    private void buildPellets() {
        pellets = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == PATH) {
                    Pellet.Type type = isPowerPosition(r, c) ? Pellet.Type.POWER : Pellet.Type.NORMAL;
                    pellets.add(new Pellet(r, c, type));
                }
            }
        }
    }

    public Pellet collectPelletAt(int row, int col) {
        for (Pellet p : pellets) {
            if (!p.isCollected() && p.getRow() == row && p.getCol() == col) {
                p.collect();
                return p;
            }
        }
        return null;
    }

    /** Apply a collected-pellets bitmask from a network snapshot. */
    public void applyCollectedMask(boolean[] mask) {
        List<Pellet> list = pellets;
        for (int i = 0; i < mask.length && i < list.size(); i++) {
            if (mask[i]) list.get(i).collect();
            else         list.get(i).reset();
        }
    }

    public boolean[] getCollectedMask() {
        boolean[] mask = new boolean[pellets.size()];
        for (int i = 0; i < pellets.size(); i++) mask[i] = pellets.get(i).isCollected();
        return mask;
    }

    public void resetPellets()              { for (Pellet p : pellets) p.reset(); }
    public int  countRemainingPellets()     { int n=0; for (Pellet p:pellets) if(!p.isCollected()) n++; return n; }
    public boolean isWall(int row, int col) { if(row<0||row>=rows||col<0||col>=cols) return true; return grid[row][col]==WALL; }
    public List<Pellet> getPellets()        { return pellets; }
    public int getCell(int r, int c)        { return grid[r][c]; }
    public int getRows()                    { return rows; }
    public int getCols()                    { return cols; }
}