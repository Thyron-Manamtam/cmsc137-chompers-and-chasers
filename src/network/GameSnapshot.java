package network;

/**
 * Serializable snapshot of the full game state sent from server → client every tick.
 * Uses a simple hand-rolled JSON format (no external libs) for easy parsing.
 *
 * Protocol: each message is a single line ending with '\n'.
 * Format:   TYPE|payload\n
 *
 * For SNAPSHOT the payload is a compact JSON object:
 * {
 *   "tick":1234,
 *   "state":"PLAYING",
 *   "pelletsCollected":[false,true,...],
 *   "chomper":{"id":0,"name":"P1","row":7,"col":7,"score":10,"lives":3,"powered":false,"powerTicks":0,"mouthFrame":2,"dir":"RIGHT"},
 *   "chasers":[
 *     {"id":1,"name":"P2","row":1,"col":1,"mode":"CHASE","frightenTicks":0,"human":true},
 *     ...
 *   ],
 *   "deathTicks":0,
 *   "countdown":3,
 *   "winnerName":""
 * }
 */
public class GameSnapshot {

    public int      tick;
    public String   state;           // GameState name
    public boolean[]pelletsCollected;// one entry per pellet in Maze order

    // Chomper data
    public int    chomperPlayerId;
    public String chomperName;
    public int    chomperRow;
    public int    chomperCol;
    public int    chomperScore;
    public int    chomperLives;
    public boolean chomperPowered;
    public int    chomperPowerTicks;
    public int    chomperMouthFrame;
    public String chomperDir;

    // Chasers (up to 3 human or AI)
    public ChaserData[] chasers;

    public int    deathTicks;
    public int    countdown;
    public String winnerName;   // populated on WIN or GAMEOVER

    public static class ChaserData {
        public int    playerId;
        public String name;
        public int    row;
        public int    col;
        public String mode;          // CHASE / FRIGHTENED / EATEN
        public int    frightenTicks;
        public boolean human;

        public ChaserData() {}
        public ChaserData(int id, String name, int row, int col, String mode, int frightenTicks, boolean human) {
            this.playerId     = id;
            this.name         = name;
            this.row          = row;
            this.col          = col;
            this.mode         = mode;
            this.frightenTicks= frightenTicks;
            this.human        = human;
        }
    }

    // ── Serialise to JSON line ───────────────────────────────

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"tick\":").append(tick).append(",");
        sb.append("\"state\":\"").append(state).append("\",");
        sb.append("\"deathTicks\":").append(deathTicks).append(",");
        sb.append("\"countdown\":").append(countdown).append(",");
        sb.append("\"winnerName\":\"").append(escape(winnerName)).append("\",");

        // pellets
        sb.append("\"pelletsCollected\":[");
        if (pelletsCollected != null) {
            for (int i = 0; i < pelletsCollected.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(pelletsCollected[i] ? "true" : "false");
            }
        }
        sb.append("],");

        // chomper
        sb.append("\"chomper\":{");
        sb.append("\"id\":").append(chomperPlayerId).append(",");
        sb.append("\"name\":\"").append(escape(chomperName)).append("\",");
        sb.append("\"row\":").append(chomperRow).append(",");
        sb.append("\"col\":").append(chomperCol).append(",");
        sb.append("\"score\":").append(chomperScore).append(",");
        sb.append("\"lives\":").append(chomperLives).append(",");
        sb.append("\"powered\":").append(chomperPowered).append(",");
        sb.append("\"powerTicks\":").append(chomperPowerTicks).append(",");
        sb.append("\"mouthFrame\":").append(chomperMouthFrame).append(",");
        sb.append("\"dir\":\"").append(escape(chomperDir)).append("\"");
        sb.append("},");

        // chasers
        sb.append("\"chasers\":[");
        if (chasers != null) {
            for (int i = 0; i < chasers.length; i++) {
                if (i > 0) sb.append(",");
                ChaserData cd = chasers[i];
                sb.append("{");
                sb.append("\"id\":").append(cd.playerId).append(",");
                sb.append("\"name\":\"").append(escape(cd.name)).append("\",");
                sb.append("\"row\":").append(cd.row).append(",");
                sb.append("\"col\":").append(cd.col).append(",");
                sb.append("\"mode\":\"").append(cd.mode).append("\",");
                sb.append("\"frightenTicks\":").append(cd.frightenTicks).append(",");
                sb.append("\"human\":").append(cd.human);
                sb.append("}");
            }
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    // ── Deserialise from JSON line ───────────────────────────

    public static GameSnapshot fromJson(String json) {
        GameSnapshot s = new GameSnapshot();
        try {
            s.tick         = parseInt(json, "tick");
            s.state        = parseStr(json, "state");
            s.deathTicks   = parseInt(json, "deathTicks");
            s.countdown    = parseInt(json, "countdown");
            s.winnerName   = parseStr(json, "winnerName");

            // pellets
            int pa = json.indexOf("\"pelletsCollected\":[");
            if (pa >= 0) {
                int start = pa + 20;
                int end   = json.indexOf(']', start);
                String arr = json.substring(start, end);
                if (!arr.trim().isEmpty()) {
                    String[] parts = arr.split(",");
                    s.pelletsCollected = new boolean[parts.length];
                    for (int i = 0; i < parts.length; i++)
                        s.pelletsCollected[i] = parts[i].trim().equals("true");
                }
            }

            // chomper
            int co = json.indexOf("\"chomper\":{");
            if (co >= 0) {
                int cs = co + 10;
                int ce = findObjEnd(json, cs);
                String cj = json.substring(cs, ce+1);
                s.chomperPlayerId  = parseInt(cj, "id");
                s.chomperName      = parseStr(cj, "name");
                s.chomperRow       = parseInt(cj, "row");
                s.chomperCol       = parseInt(cj, "col");
                s.chomperScore     = parseInt(cj, "score");
                s.chomperLives     = parseInt(cj, "lives");
                s.chomperPowered   = parseBool(cj, "powered");
                s.chomperPowerTicks= parseInt(cj, "powerTicks");
                s.chomperMouthFrame= parseInt(cj, "mouthFrame");
                s.chomperDir       = parseStr(cj, "dir");
            }

            // chasers array
            int cha = json.indexOf("\"chasers\":[");
            if (cha >= 0) {
                int start = cha + 10;
                // find matching ] for the outer array
                java.util.List<ChaserData> list = new java.util.ArrayList<>();
                int i = start;
                while (i < json.length()) {
                    int ob = json.indexOf('{', i);
                    if (ob < 0) break;
                    int oe = findObjEnd(json, ob);
                    if (oe < 0) break;
                    String cj2 = json.substring(ob, oe+1);
                    ChaserData cd = new ChaserData();
                    cd.playerId     = parseInt(cj2, "id");
                    cd.name         = parseStr(cj2, "name");
                    cd.row          = parseInt(cj2, "row");
                    cd.col          = parseInt(cj2, "col");
                    cd.mode         = parseStr(cj2, "mode");
                    cd.frightenTicks= parseInt(cj2, "frightenTicks");
                    cd.human        = parseBool(cj2, "human");
                    list.add(cd);
                    i = oe + 1;
                    // stop if we hit ']' for the chasers array
                    int nextBrace = json.indexOf('{', i);
                    int nextBracket = json.indexOf(']', i);
                    if (nextBracket < 0 || (nextBrace > 0 && nextBrace < nextBracket)) continue;
                    break;
                }
                s.chasers = list.toArray(new ChaserData[0]);
            }
        } catch (Exception e) {
            // malformed packet — return partial
        }
        return s;
    }

    // ── Mini JSON helpers (no external deps) ─────────────────

    private static int parseInt(String json, String key) {
        String pat = "\"" + key + "\":";
        int idx = json.indexOf(pat);
        if (idx < 0) return 0;
        int start = idx + pat.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return 0; }
    }

    private static boolean parseBool(String json, String key) {
        String pat = "\"" + key + "\":";
        int idx = json.indexOf(pat);
        if (idx < 0) return false;
        int start = idx + pat.length();
        return json.startsWith("true", start);
    }

    private static String parseStr(String json, String key) {
        String pat = "\"" + key + "\":\"";
        int idx = json.indexOf(pat);
        if (idx < 0) return "";
        int start = idx + pat.length();
        int end = json.indexOf('"', start);
        if (end < 0) return "";
        return json.substring(start, end).replace("\\\"", "\"");
    }

    private static int findObjEnd(String json, int openBrace) {
        int depth = 0;
        for (int i = openBrace; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}