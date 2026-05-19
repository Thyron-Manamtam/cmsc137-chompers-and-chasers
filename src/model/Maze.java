package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Maze {
    public static final int WALL = 1;
    public static final int PATH = 0;

    // All valid open-path cells that are NOT at the corners (reserved for chaser spawns)
    // Super-pellet relocation picks 4 random PATH cells each time
    private static final int[][] CHASER_SPAWN_POSITIONS = { {1,1},{1,13},{13,1},{13,13} };
    // Default power positions on first spawn
    private static final int[][] DEFAULT_POWER_POSITIONS = { {1,1},{1,13},{13,1},{13,13} };

    private final int[][] grid;
    private List<Pellet> pellets;
    private final int rows, cols;
    private final Random random = new Random();

    // Current power-pellet positions (may change on relocation)
    private int[][] currentPowerPositions;

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
        currentPowerPositions = DEFAULT_POWER_POSITIONS;
        buildPellets();
    }

    private boolean isPowerPosition(int r, int c) {
        for (int[] pos : currentPowerPositions)
            if (pos[0] == r && pos[1] == c) return true;
        return false;
    }

    private boolean isChaserSpawn(int r, int c) {
        for (int[] pos : CHASER_SPAWN_POSITIONS)
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

    /**
     * Relocates the 4 power pellets to 4 new random PATH positions (not chaser spawns,
     * not current power positions, not already-collected cells).
     * Existing power pellets are demoted to NORMAL; new cells are promoted to POWER.
     * Returns the new power-pellet positions so the caller can announce them.
     */
    public int[][] relocatePowerPellets() {
        // Build candidate list: PATH cells that aren't chaser spawns
        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == PATH && !isChaserSpawn(r, c))
                    candidates.add(new int[]{r, c});

        Collections.shuffle(candidates, random);

        // Pick 4 unique positions
        List<int[]> newPositions = new ArrayList<>();
        for (int[] cell : candidates) {
            if (newPositions.size() == 4) break;
            newPositions.add(cell);
        }

        // Demote old power pellets → NORMAL
        for (Pellet p : pellets)
            if (p.isPower()) p.demoteToPower(false);

        // Promote new positions → POWER (add uncollected pellet if cell was collected)
        currentPowerPositions = newPositions.toArray(new int[0][]);
        for (int[] pos : currentPowerPositions) {
            boolean found = false;
            for (Pellet p : pellets) {
                if (p.getRow() == pos[0] && p.getCol() == pos[1]) {
                    p.demoteToPower(true);   // re-promote; also un-collects it
                    found = true;
                    break;
                }
            }
            if (!found) {
                pellets.add(new Pellet(pos[0], pos[1], Pellet.Type.POWER));
            }
        }

        return currentPowerPositions;
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