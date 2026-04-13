package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.MessageManager;
import manager.UserManager;
import model.Student;

import java.io.IOException;

/**
 * Handles authentication endpoints:
 *   POST /api/auth/login
 *   POST /api/auth/register
 *   POST /api/auth/logout
 */
public class AuthHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        if (method.equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            if (path.endsWith("/login") && method.equals("POST")) {
                handleLogin(ex);
            } else if (path.endsWith("/register") && method.equals("POST")) {
                handleRegister(ex);
            } else if (path.endsWith("/logout") && method.equals("POST")) {
                handleLogout(ex);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        String username = JsonUtil.parseString(body, "username");
        String password = JsonUtil.parseString(body, "password");

        if (username == null || password == null) {
            sendError(ex, 400, "username and password are required");
            return;
        }

        Student student = UserManager.getInstance().login(username, password);
        if (student == null) {
            sendError(ex, 401, "Invalid username or password");
            return;
        }

        // Register as observer (mirrors LoginCommand behaviour)
        MessageManager.getInstance().registerObserver(student);

        String token = SessionManager.getInstance().createSession(student);

        sendJson(ex, 200, "{" +
            "\"token\":\"" + JsonUtil.escape(token) + "\"," +
            "\"username\":\"" + JsonUtil.escape(student.getUsername()) + "\"," +
            "\"role\":\"" + JsonUtil.escape(student.getRole()) + "\"," +
            "\"status\":\"" + JsonUtil.escape(student.getStateName()) + "\"" +
        "}");
    }

    private void handleRegister(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        String username = JsonUtil.parseString(body, "username");
        String password = JsonUtil.parseString(body, "password");
        String major    = JsonUtil.parseString(body, "major");

        if (username == null || password == null || major == null) {
            sendError(ex, 400, "username, password, and major are required");
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            sendError(ex, 400, "username and password cannot be blank");
            return;
        }

        boolean success = UserManager.getInstance().register(username, password, major);
        if (!success) {
            sendError(ex, 409, "Username already taken");
            return;
        }
        sendJson(ex, 200, "{\"success\":true}");
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) token = auth.substring(7).trim();

        Student student = SessionManager.getInstance().getStudent(token);
        if (student != null) {
            MessageManager.getInstance().removeObserver(student);
            SessionManager.getInstance().removeSession(token);
        }
        sendJson(ex, 200, "{\"success\":true}");
    }
}
