package util;

/**
 * Central configuration for the game.
 * Tweak values here without touching game logic.
 *
 * Milestone 2: many of these will be negotiated with the server.
 */
public final class GameConfig {
    private GameConfig() {}

    // Display
    public static final int TILE_SIZE       = 40;   // pixels per grid cell
    public static final int MAZE_ROWS       = 15;
    public static final int MAZE_COLS       = 15;

    // Timing
    public static final int TICK_MS         = 250;  // game loop interval in ms
    public static final int DEATH_TICKS     = 12;   // freeze frames on death

    // Player
    public static final int MAX_LIVES       = 3;
    public static final int POWER_DURATION  = 63;   // ticks
    public static final int SCORE_PELLET    = 10;
    public static final int SCORE_POWER     = 50;
    public static final int SCORE_EAT_BASE  = 200;  // doubles per chaser eaten in one power

    // Chasers (Milestone 1 AI only)
    public static final int NUM_AI_CHASERS  = 4;

    // Milestone 2 networking (placeholders)
    public static final int    SERVER_PORT      = 5000;
    public static final String DEFAULT_HOST     = "localhost";
    public static final int    MIN_PLAYERS      = 4;
    public static final int    MAX_PLAYERS      = 4;
}
