package util;

public final class GameConfig {
    private GameConfig() {}

    public static final int TILE_SIZE       = 40;
    public static final int MAZE_ROWS       = 15;
    public static final int MAZE_COLS       = 15;
    public static final int TICK_MS         = 150;   // was 250 — smoother movement
    public static final int DEATH_TICKS     = 20;    // scaled with new tick rate
    public static final int MAX_LIVES       = 3;
    public static final int POWER_DURATION  = 100;   // scaled with new tick rate (~15 s)
    public static final int SCORE_PELLET    = 10;
    public static final int SCORE_POWER     = 50;
    public static final int SCORE_EAT_BASE  = 200;
    public static final int NUM_AI_CHASERS  = 4;
    public static final int SERVER_PORT     = 5000;
    public static final String DEFAULT_HOST = "localhost";
    public static final int MIN_PLAYERS     = 2;
    public static final int MAX_PLAYERS     = 5;
    public static final int GAME_DURATION_S = 120;
    public static final int ROLE_REVEAL_MS  = 4000;

    // Super-pellet relocation schedule (seconds elapsed when pellets move)
    // Pellets relocate at 0:30, 1:00, 1:30 → game clock at those moments
    // timeLeft counts DOWN from GAME_DURATION_S, so relocation triggers when:
    //   timeLeft == GAME_DURATION_S - 30  => 90
    //   timeLeft == GAME_DURATION_S - 60  => 60
    //   timeLeft == GAME_DURATION_S - 90  => 30
    public static final int[] SUPER_PELLET_RELOCATE_AT = { 90, 60, 30 };
    // Warn 3 seconds before each relocation
    public static final int SUPER_PELLET_WARN_BEFORE_S = 3;

    // Unified window size for all non-game screens
    public static final int WINDOW_W = 600;
    public static final int WINDOW_H = 700;

    // Game window size
    public static final int GAME_W  = MAZE_COLS * TILE_SIZE;           // 600
    public static final int GAME_H  = MAZE_ROWS * TILE_SIZE + 30;      // 630
}