package util;

public enum GameState {
    START,       // Title screen (Single Player / Multiplayer)
    MP_MENU,     // Multiplayer menu (Host Game / Join Game)
    JOINING,     // Connecting to server
    LOBBY,       // Waiting for players
    ROLE_REVEAL, // Role has been assigned, showing role card
    COUNTDOWN,   // All ready, counting down
    PLAYING,     // Active gameplay
    PAUSED,      // Paused (single-player only)
    DEAD,        // Brief death animation
    GAME_OVER,   // All lives lost
    WIN,         // All pellets collected / chasers won
    ERROR,       // Connection error
    DISCONNECTED // Lost connection mid-game
}