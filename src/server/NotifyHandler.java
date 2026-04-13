package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Student;

import java.io.IOException;
import java.util.List;

/**
 * Handles notification polling (OBSERVER PATTERN adaptation for web):
 *   GET /api/notifications/poll
 *
 * The frontend calls this every 5 seconds. It drains the Student's
 * pendingNotifications queue and returns the items as a JSON array.
 * This adapts the existing in-memory Observer queue to work over HTTP.
 */
public class NotifyHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            // Drain the Observer queue into a list (thread-safe via ConcurrentLinkedQueue)
            List<String> notifications = student.drainNotificationsAsList();

            List<String> escaped = new java.util.ArrayList<>();
            for (String n : notifications) {
                escaped.add("\"" + JsonUtil.escape(n) + "\"");
            }

            StringBuilder sb = new StringBuilder("{\"notifications\":[");
            for (int i = 0; i < escaped.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escaped.get(i));
            }
            sb.append("]}");

            sendJson(ex, 200, sb.toString());
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }
}
