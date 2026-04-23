package network;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║          MILESTONE 2 — NOT YET IMPLEMENTED               ║
 * ║                                                          ║
 * ║  GameServer will:                                        ║
 * ║   • Accept TCP socket connections on GameConfig.PORT     ║
 * ║   • Wait for MIN_PLAYERS (4) before starting            ║
 * ║   • Assign roles (Chomper / Chaser) to each client       ║
 * ║   • Run the authoritative game loop                      ║
 * ║   • Broadcast GameSnapshot to all clients each tick      ║
 * ║   • Detect collisions, wins, and game-over server-side   ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Architecture sketch:
 *
 *   ServerSocket  ──►  one ClientHandler thread per player
 *                       ClientHandler reads input (Direction)
 *                       and writes GameSnapshot (JSON / binary)
 *
 * The existing model classes (Maze, Player, Chaser) are already
 * designed without Swing dependencies so they can run on the server.
 */
public class GameServer {
    // TODO Milestone 2
    public static void main(String[] args) {
        System.out.println("GameServer: Milestone 2 — not yet implemented.");
    }
}
