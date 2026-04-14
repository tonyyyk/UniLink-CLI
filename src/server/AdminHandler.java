package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.UserManager;
import model.Student;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles admin-only endpoints (requires ADMIN role):
 *   GET  /api/admin/users      — list all users
 *   POST /api/admin/suspend    — suspend a user
 *   POST /api/admin/reinstate  — reinstate a user
 */
public class AdminHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            if (!student.isAdmin()) {
                sendError(ex, 403, "Admin access required");
                return;
            }

            String path = ex.getRequestURI().getPath();

            if (path.endsWith("/users") && method.equals("GET")) {
                handleListUsers(ex);
            } else if (path.endsWith("/suspend") && method.equals("POST")) {
                handleSuspend(ex, student);
            } else if (path.endsWith("/reinstate") && method.equals("POST")) {
                handleReinstate(ex);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleListUsers(HttpExchange ex) throws IOException {
        List<Student> users = UserManager.getInstance().getAllUsers();
        List<String> items = new ArrayList<>();
        for (Student s : users) {
            items.add("{" +
                "\"username\":\"" + JsonUtil.escape(s.getUsername()) + "\"," +
                "\"major\":\"" + JsonUtil.escape(s.getMajor()) + "\"," +
                "\"role\":\"" + JsonUtil.escape(s.getRole()) + "\"," +
                "\"status\":\"" + JsonUtil.escape(s.getStateName()) + "\"" +
            "}");
        }
        sendJson(ex, 200, "{\"users\":" + JsonUtil.jsonArray(items) + "}");
    }

    private void handleSuspend(HttpExchange ex, Student admin) throws IOException {
        String body = readBody(ex);
        String username = JsonUtil.parseString(body, "username");

        if (username == null || username.isBlank()) {
            sendError(ex, 400, "username is required");
            return;
        }
        if (username.equalsIgnoreCase(admin.getUsername())) {
            sendError(ex, 400, "You cannot suspend your own account");
            return;
        }
        if (!UserManager.getInstance().usernameExists(username)) {
            sendError(ex, 404, "User not found");
            return;
        }
        UserManager.getInstance().suspendUser(username);
        SessionManager.getInstance().suspendActiveSession(username);   // instant effect
        sendJson(ex, 200, "{\"success\":true}");
    }

    private void handleReinstate(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        String username = JsonUtil.parseString(body, "username");

        if (username == null || username.isBlank()) {
            sendError(ex, 400, "username is required");
            return;
        }
        if (!UserManager.getInstance().usernameExists(username)) {
            sendError(ex, 404, "User not found");
            return;
        }
        UserManager.getInstance().reinstateUser(username);
        SessionManager.getInstance().reinstateActiveSession(username); // instant effect
        sendJson(ex, 200, "{\"success\":true}");
    }
}
