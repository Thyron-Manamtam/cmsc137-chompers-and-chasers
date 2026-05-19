package network;

import util.Role;
import java.util.*;

/** Snapshot of game state received from server — used by client-side renderer. */
public class ClientGameState {
    public static class PlayerInfo {
        public int id;
        public String name;
        public Role role;
        public int row, col;
        public int lives, score;
        public boolean powered;
        public String direction;
        public boolean connected;
    }

    public static class PelletInfo {
        public int row, col;
        public boolean power;
        public boolean collected;
    }

    public List<PlayerInfo> players = new ArrayList<>();
    public List<PelletInfo> pellets = new ArrayList<>();
    public int timeLeft = 120;
    public int tick = 0;
    public String gameResult = null; // null = in progress, "CHOMPERS" or "CHASERS"
    public String resultReason = null;

    // Local player info
    public int myId = -1;
    public Role myRole = null;
    public String myName = "";

    public PlayerInfo getMyInfo() {
        for (PlayerInfo p : players)
            if (p.id == myId) return p;
        return null;
    }
}
