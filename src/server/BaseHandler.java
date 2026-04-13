package server;

import com.sun.net.httpserver.HttpExchange;
import model.Student;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base for all API handlers.
 * Provides shared utilities: authentication, CORS headers, JSON response writing.
 */
public abstract class BaseHandler {

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Extract the Bearer token from the Authorization header and look up the session.
     * Sends HTTP 401 and returns null if the token is missing or invalid.
     */
    protected Student authenticate(HttpExchange ex) throws IOException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        Student student = SessionManager.getInstance().getStudent(token);
        if (student == null) {
            sendError(ex, 401, "Unauthorized — please log in");
            return null;
        }
        return student;
    }

    // ── Request helpers ───────────────────────────────────────────────────────

    /** Read the full request body as a UTF-8 string. */
    protected String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Extract a query parameter value by name from the URL query string. */
    protected String getQueryParam(HttpExchange ex, String name) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) return kv[1];
        }
        return null;
    }

    // ── Response helpers ──────────────────────────────────────────────────────

    /** Send a JSON response with the given HTTP status code. */
    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        addCorsHeaders(ex);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Convenience method: send {"error":"..."} with the given status. */
    protected void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, "{\"error\":\"" + JsonUtil.escape(message) + "\"}");
    }

    /** Handle CORS preflight OPTIONS request — always returns 200 with no body. */
    protected void handleOptions(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        ex.sendResponseHeaders(200, -1);
    }

    // ── CORS ──────────────────────────────────────────────────────────────────

    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
    }
}
