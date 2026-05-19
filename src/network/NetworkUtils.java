package network;

import java.net.*;
import java.util.*;

public class NetworkUtils {

    public static String getLocalIP() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 80);
            String ip = socket.getLocalAddress().getHostAddress();
            if (ip != null && !ip.equals("0.0.0.0") && !ip.equals("127.0.0.1")) return ip;
        } catch (Exception ignored) {}

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!(addr instanceof Inet4Address)) continue;
                    String ip = addr.getHostAddress();
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) return ip;
                    if (fallback == null) fallback = ip;
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception e) { e.printStackTrace(); }
        return "127.0.0.1";
    }

    public static void printDiagnostic() {
        System.out.println("=== NETWORK DIAGNOSTIC ===");
        System.out.println("Chosen IP (getLocalIP): " + getLocalIP());
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                System.out.printf("  [%s] up=%b loopback=%b virtual=%b%n",
                    ni.getName(), ni.isUp(), ni.isLoopback(), ni.isVirtual());
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements())
                    System.out.println("    -> " + addresses.nextElement().getHostAddress());
            }
        } catch (Exception e) { System.out.println("  Error: " + e.getMessage()); }
        System.out.println("==========================");
    }

    public static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":").append(valueToJson(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String valueToJson(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return "\"" + ((String)v).replace("\"","\\\"") + "\"";
        if (v instanceof Boolean || v instanceof Number) return v.toString();
        if (v instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (List<?>)v) { if (!first) sb.append(","); first = false; sb.append(valueToJson(item)); }
            sb.append("]");
            return sb.toString();
        }
        if (v instanceof Map) { @SuppressWarnings("unchecked") Map<String,Object> m = (Map<String,Object>)v; return toJson(m); }
        return "\"" + v + "\"";
    }

    public static Map<String, Object> fromJson(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        for (String pair : splitTopLevel(json, ',')) {
            int colon = pair.indexOf(':');
            if (colon < 0) continue;
            String key = pair.substring(0, colon).trim().replace("\"","");
            String val = pair.substring(colon + 1).trim();
            map.put(key, parseValue(val));
        }
        return map;
    }

    private static Object parseValue(String val) {
        if (val.equals("null")) return null;
        if (val.equals("true")) return true;
        if (val.equals("false")) return false;
        if (val.startsWith("\"") && val.endsWith("\"")) return val.substring(1, val.length()-1).replace("\\\"","\"");
        if (val.startsWith("[")) {
            List<Object> list = new ArrayList<>();
            String inner = val.substring(1, val.length()-1).trim();
            if (!inner.isEmpty()) for (String item : splitTopLevel(inner, ',')) list.add(parseValue(item.trim()));
            return list;
        }
        if (val.startsWith("{")) return fromJson(val);
        try { return Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        try { return Long.parseLong(val); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(val); } catch (NumberFormatException ignored) {}
        return val;
    }

    private static List<String> splitTopLevel(String s, char delim) {
        List<String> parts = new ArrayList<>();
        int depth = 0; boolean inStr = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i-1) != '\\')) inStr = !inStr;
            if (!inStr) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == delim && depth == 0) { parts.add(cur.toString()); cur.setLength(0); continue; }
            }
            cur.append(c);
        }
        if (cur.length() > 0) parts.add(cur.toString());
        return parts;
    }
}