package util;

public final class GameConfig {
    private GameConfig() {}

    public static final int TILE_SIZE       = 40;
    public static final int MAZE_ROWS       = 15;
    public static final int MAZE_COLS       = 15;
    public static final int TICK_MS         = 250;
    public static final int DEATH_TICKS     = 12;
    public static final int MAX_LIVES       = 3;
    public static final int POWER_DURATION  = 63;
    public static final int SCORE_PELLET    = 10;
    public static final int SCORE_POWER     = 50;
    public static final int SCORE_EAT_BASE  = 200;
    public static final int NUM_AI_CHASERS  = 4;
    public static final int    SERVER_PORT      = 5000;
    public static final String DEFAULT_HOST     = "localhost";
    public static final int    MIN_PLAYERS      = 2;
    public static final int    MAX_PLAYERS      = 5;
    public static final int    GAME_DURATION_S  = 120;
    public static final int    ROLE_REVEAL_MS   = 3000;
}
