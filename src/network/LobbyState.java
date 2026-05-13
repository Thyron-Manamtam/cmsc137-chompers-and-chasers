package network;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent from server → all clients whenever the lobby changes.
 * Protocol line: LOBBY|{json}
 */
public class LobbyState {

    public static class PlayerInfo {
        public int    id;
        public String name;
        public String role;   // "CHOMPER", "CHASER", or "TBD"
        public boolean ready;

        public PlayerInfo() {}
        public PlayerInfo(int id, String name, String role, boolean ready) {
            this.id = id; this.name = name; this.role = role; this.ready = ready;
        }
    }

    public List<PlayerInfo> players = new ArrayList<>();
    public int hostId;

    // ── Serialise ────────────────────────────────────────────

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"hostId\":").append(hostId).append(",\"players\":[");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(",");
            PlayerInfo p = players.get(i);
            sb.append("{\"id\":").append(p.id)
              .append(",\"name\":\"").append(p.name).append("\"")
              .append(",\"role\":\"").append(p.role).append("\"")
              .append(",\"ready\":").append(p.ready)
              .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ── Deserialise ──────────────────────────────────────────

    public static LobbyState fromJson(String json) {
        LobbyState ls = new LobbyState();
        try {
            // hostId
            ls.hostId = parseInt(json, "hostId");
            // players array
            int start = json.indexOf("[");
            int end   = json.lastIndexOf("]");
            if (start < 0 || end < 0) return ls;
            String arr = json.substring(start + 1, end);
            // split on },{
            int i = 0;
            while (i < arr.length()) {
                int ob = arr.indexOf('{', i);
                if (ob < 0) break;
                int oe = arr.indexOf('}', ob);
                if (oe < 0) break;
                String obj = arr.substring(ob, oe + 1);
                PlayerInfo pi = new PlayerInfo();
                pi.id    = parseInt(obj, "id");
                pi.name  = parseStr(obj, "name");
                pi.role  = parseStr(obj, "role");
                pi.ready = parseBool(obj, "ready");
                ls.players.add(pi);
                i = oe + 1;
            }
        } catch (Exception ignored) {}
        return ls;
    }

    private static int parseInt(String json, String key) {
        String pat = "\"" + key + "\":";
        int idx = json.indexOf(pat);
        if (idx < 0) return 0;
        int s = idx + pat.length();
        int e = s;
        while (e < json.length() && Character.isDigit(json.charAt(e))) e++;
        try { return Integer.parseInt(json.substring(s, e)); } catch (Exception ex) { return 0; }
    }

    private static boolean parseBool(String json, String key) {
        String pat = "\"" + key + "\":";
        int idx = json.indexOf(pat);
        if (idx < 0) return false;
        return json.startsWith("true", idx + pat.length());
    }

    private static String parseStr(String json, String key) {
        String pat = "\"" + key + "\":\"";
        int idx = json.indexOf(pat);
        if (idx < 0) return "";
        int s = idx + pat.length();
        int e = json.indexOf('"', s);
        if (e < 0) return "";
        return json.substring(s, e);
    }
}