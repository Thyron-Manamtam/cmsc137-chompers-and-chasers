package model;

import java.util.ArrayList;
import java.util.List;

public class Maze {
    public static final int WALL = 1;
    public static final int PATH = 0;

    private static final int[][] POWER_POSITIONS = { {1,1},{1,13},{13,1},{13,13} };

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
        for (int[] pos : POWER_POSITIONS)
            if (pos[0] == r && pos[1] == c) return true;
        return false;
    }

    private void buildPellets() {
        pellets = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == PATH)
                    pellets.add(new Pellet(r, c, isPowerPosition(r,c) ? Pellet.Type.POWER : Pellet.Type.NORMAL));
    }

    public Pellet collectPelletAt(int row, int col) {
        for (Pellet p : pellets)
            if (!p.isCollected() && p.getRow() == row && p.getCol() == col) { p.collect(); return p; }
        return null;
    }

    public void resetPellets() { for (Pellet p : pellets) p.reset(); }

    public int countRemainingPellets() {
        int count = 0;
        for (Pellet p : pellets) if (!p.isCollected()) count++;
        return count;
    }

    public boolean isWall(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return true;
        return grid[row][col] == WALL;
    }

    public List<Pellet> getPellets() { return pellets; }
    public int getCell(int row, int col) { return grid[row][col]; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
}