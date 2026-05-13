package util;

/** Which mode the application is running in. */
public enum AppMode {
    SINGLE_PLAYER,  // Local game with AI chasers
    HOST,           // This instance is running the server AND a client
    CLIENT          // This instance is a pure network client
}