package server;

import model.Message;
import model.Student;
import model.StudyGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for manual JSON serialisation and parsing.
 * No external libraries — uses StringBuilder for output and indexOf for input.
 */
public class JsonUtil {

    private JsonUtil() {}

    // ── String escaping ───────────────────────────────────────────────────────

    /** Escape backslashes and double-quotes for use inside a JSON string value. */
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    /** Serialise a Student to a JSON profile object. */
    public static String toJson(Student s) {
        return "{" +
            "\"username\":\"" + escape(s.getUsername()) + "\"," +
            "\"major\":\"" + escape(s.getMajor()) + "\"," +
            "\"role\":\"" + escape(s.getRole()) + "\"," +
            "\"status\":\"" + escape(s.getStateName()) + "\"," +
            "\"canSendMessage\":" + s.canSendMessage() + "," +
            "\"canAppearInSearch\":" + s.canAppearInSearch() + "," +
            "\"strengths\":" + toJsonStringArray(s.getStrengths()) + "," +
            "\"weaknesses\":" + toJsonStringArray(s.getWeaknesses()) +
        "}";
    }

    /** Serialise a Message to a JSON object. */
    public static String toJson(Message m) {
        return "{" +
            "\"sender\":\"" + escape(m.getSender()) + "\"," +
            "\"receiver\":\"" + escape(m.getReceiver()) + "\"," +
            "\"timestamp\":\"" + escape(m.getFormattedTimestamp()) + "\"," +
            "\"content\":\"" + escape(m.getContent()) + "\"," +
            "\"read\":" + m.isRead() +
        "}";
    }

    /** Serialise a StudyGroup to a JSON object, including an isMember flag. */
    public static String toJson(StudyGroup g, String currentUser) {
        return "{" +
            "\"id\":" + g.getGroupId() + "," +
            "\"name\":\"" + escape(g.getGroupName()) + "\"," +
            "\"creator\":\"" + escape(g.getCreator()) + "\"," +
            "\"topic\":\"" + escape(g.getTopic()) + "\"," +
            "\"members\":" + toJsonStringArray(g.getMembers()) + "," +
            "\"isMember\":" + g.hasMember(currentUser) +
        "}";
    }

    /** Wrap a List of pre-built JSON object strings into a JSON array. */
    public static String jsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(items.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    /** Serialise a List<String> as a JSON string array. */
    public static String toJsonStringArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(items.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── Parsing (inbound JSON from browser) ───────────────────────────────────

    /**
     * Extract a string value from a flat JSON object body.
     * Example: parseString("{\"username\":\"alice\"}", "username") → "alice"
     * Returns null if the key is not found.
     */
    public static String parseString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(":", keyIdx + search.length());
        if (colon < 0) return null;
        // Skip whitespace after colon
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length() || json.charAt(start) != '"') return null;
        start++; // skip opening quote
        // Find closing quote, respecting escaped quotes
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && (end == 0 || json.charAt(end - 1) != '\\')) break;
            end++;
        }
        return json.substring(start, end);
    }

    /**
     * Extract an integer value from a flat JSON object body.
     * Returns -1 if the key is not found or parsing fails.
     */
    public static int parseInt(String json, String key) {
        if (json == null) return -1;
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return -1;
        int colon = json.indexOf(":", keyIdx + search.length());
        if (colon < 0) return -1;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Extract a string array from a JSON body.
     * Example: parseStringArray("{\"strengths\":[\"Java\",\"Math\"]}", "strengths")
     * Returns empty list if key is absent.
     */
    public static List<String> parseStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        if (json == null) return result;
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return result;
        int arrStart = json.indexOf("[", keyIdx);
        int arrEnd = json.indexOf("]", arrStart);
        if (arrStart < 0 || arrEnd < 0) return result;
        String inner = json.substring(arrStart + 1, arrEnd).trim();
        if (inner.isEmpty()) return result;
        // Split on "," boundaries between quoted strings
        for (String token : inner.split(",")) {
            String val = token.trim().replaceAll("^\"|\"$", "");
            if (!val.isEmpty()) result.add(val);
        }
        return result;
    }
}
