package util;

public final class GameConfig {
    private GameConfig() {}

    // Display
    public static final int TILE_SIZE       = 40;
    public static final int MAZE_ROWS       = 15;
    public static final int MAZE_COLS       = 15;

    // Timing
    public static final int TICK_MS         = 250;
    public static final int DEATH_TICKS     = 12;
    public static final int COUNTDOWN_TICKS = 3;   // seconds before game starts

    // Player
    public static final int MAX_LIVES       = 3;
    public static final int POWER_DURATION  = 63;
    public static final int SCORE_PELLET    = 10;
    public static final int SCORE_POWER     = 50;
    public static final int SCORE_EAT_BASE  = 200;

    // Chasers (single-player AI)
    public static final int NUM_AI_CHASERS  = 4;

    // Networking
    public static int    SERVER_PORT      = 5000;
    public static String DEFAULT_HOST     = "localhost";
    public static int    MIN_PLAYERS      = 2;
    public static int    MAX_PLAYERS      = 5;

    // Protocol message types (sent over TCP as JSON lines)
    public static final String MSG_HELLO        = "HELLO";    // client → server: name
    public static final String MSG_LOBBY_UPDATE = "LOBBY";    // server → client: lobby state
    public static final String MSG_ROLE_ASSIGN  = "ROLE";     // server → client: assigned role
    public static final String MSG_READY        = "READY";    // client → server: ready
    public static final String MSG_COUNTDOWN    = "COUNTDOWN";// server → client: count
    public static final String MSG_SNAPSHOT     = "SNAPSHOT"; // server → client: game state
    public static final String MSG_INPUT        = "INPUT";    // client → server: direction
    public static final String MSG_GAME_OVER    = "GAMEOVER"; // server → client
    public static final String MSG_WIN          = "WIN";      // server → client
    public static final String MSG_ERROR        = "ERROR";    // server → client
    public static final String MSG_DISCONNECT   = "DISCONNECT";// server → client (player left)
    public static final String MSG_PING         = "PING";
    public static final String MSG_PONG         = "PONG";
}