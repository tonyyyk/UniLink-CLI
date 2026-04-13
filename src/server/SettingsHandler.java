package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.MessageManager;
import manager.UserManager;
import model.Student;

import java.io.IOException;

/**
 * Handles account self-management endpoints:
 *   POST /api/settings/password — change own password
 *   POST /api/settings/delete   — delete own account
 */
public class SettingsHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/password")) {
                handleChangePassword(ex, student);
            } else if (path.endsWith("/delete")) {
                handleDeleteAccount(ex, student);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    /** POST /api/settings/password — {oldPassword, newPassword} */
    private void handleChangePassword(HttpExchange ex, Student student) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { sendError(ex, 405, "POST required"); return; }

        String body        = readBody(ex);
        String oldPassword = JsonUtil.parseString(body, "oldPassword");
        String newPassword = JsonUtil.parseString(body, "newPassword");

        if (oldPassword == null || newPassword == null || newPassword.isBlank()) {
            sendError(ex, 400, "oldPassword and newPassword are required"); return;
        }
        if (newPassword.length() < 4) {
            sendError(ex, 400, "New password must be at least 4 characters"); return;
        }

        boolean ok = UserManager.getInstance().changePassword(
                student.getUsername(), oldPassword, newPassword);
        if (!ok) {
            sendError(ex, 403, "Current password is incorrect"); return;
        }

        // Invalidate session so user must re-login with new password
        String token = extractToken(ex);
        if (token != null) SessionManager.getInstance().removeSession(token);

        sendJson(ex, 200, "{\"success\":true,\"message\":\"Password changed. Please log in again.\"}");
    }

    /** POST /api/settings/delete — {password} */
    private void handleDeleteAccount(HttpExchange ex, Student student) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { sendError(ex, 405, "POST required"); return; }

        String body     = readBody(ex);
        String password = JsonUtil.parseString(body, "password");

        if (password == null || password.isBlank()) {
            sendError(ex, 400, "password is required"); return;
        }

        boolean ok = UserManager.getInstance().deleteAccount(student.getUsername(), password);
        if (!ok) {
            sendError(ex, 403, "Incorrect password or cannot delete the last admin account"); return;
        }

        // Remove observer and session
        MessageManager.getInstance().removeObserver(student);
        String token = extractToken(ex);
        if (token != null) SessionManager.getInstance().removeSession(token);

        sendJson(ex, 200, "{\"success\":true,\"message\":\"Account deleted.\"}");
    }

    /** Extract the Bearer token from the Authorization header (may return null). */
    private String extractToken(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();
        return null;
    }
}
