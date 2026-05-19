package network;

import util.Role;
import java.util.*;

public class ClientGameState {
    public static class PlayerInfo {
        public int id;
        public String name;
        public Role role;
        public int row, col;
        public int lives, score;
        public boolean powered;
        public boolean ready;
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
    public String gameResult = null;
    public String resultReason = null;

    public int myId = -1;
    public Role myRole = null;
    public String myName = "";
    public int hostId = -1;

    public PlayerInfo getMyInfo() {
        for (PlayerInfo p : players) if (p.id == myId) return p;
        return null;
    }
}