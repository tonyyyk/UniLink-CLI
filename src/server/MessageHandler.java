package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.MessageManager;
import manager.UserManager;
import model.Message;
import model.Student;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles messaging endpoints:
 *   GET  /api/messages/unread-count
 *   GET  /api/messages/conversation?with=<username>
 *   POST /api/messages/send
 *   GET  /api/messages/contacts
 */
public class MessageHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            String path = ex.getRequestURI().getPath();
            // Route on the last path segment
            if (path.endsWith("/unread-count")) {
                handleUnreadCount(ex, student);
            } else if (path.endsWith("/conversation")) {
                handleConversation(ex, student);
            } else if (path.endsWith("/send")) {
                handleSend(ex, student);
            } else if (path.endsWith("/contacts")) {
                handleContacts(ex, student);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleUnreadCount(HttpExchange ex, Student student) throws IOException {
        int count = MessageManager.getInstance()
                .getUnreadForUser(student.getUsername()).size();
        sendJson(ex, 200, "{\"count\":" + count + "}");
    }

    private void handleConversation(HttpExchange ex, Student student) throws IOException {
        String partner = getQueryParam(ex, "with");
        if (partner == null || partner.isBlank()) {
            sendError(ex, 400, "Query parameter 'with' is required");
            return;
        }

        List<Message> messages = MessageManager.getInstance()
                .getConversation(student.getUsername(), partner);

        // Mark messages from the partner as read
        MessageManager.getInstance().markConversationAsRead(student.getUsername(), partner);

        List<String> msgJsons = new ArrayList<>();
        for (Message m : messages) msgJsons.add(JsonUtil.toJson(m));

        sendJson(ex, 200, "{" +
            "\"partner\":\"" + JsonUtil.escape(partner) + "\"," +
            "\"messages\":" + JsonUtil.jsonArray(msgJsons) +
        "}");
    }

    private void handleSend(HttpExchange ex, Student student) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) {
            sendError(ex, 405, "POST required"); return;
        }
        if (!student.canSendMessage()) {
            sendError(ex, 403, "Your account is SUSPENDED. You cannot send messages.");
            return;
        }

        String body = readBody(ex);
        String to      = JsonUtil.parseString(body, "to");
        String content = JsonUtil.parseString(body, "content");

        if (to == null || content == null || content.isBlank()) {
            sendError(ex, 400, "to and content are required");
            return;
        }
        if (!UserManager.getInstance().usernameExists(to)) {
            sendError(ex, 404, "Recipient '" + to + "' does not exist");
            return;
        }
        if (to.equalsIgnoreCase(student.getUsername())) {
            sendError(ex, 400, "You cannot message yourself");
            return;
        }

        // Replace commas to keep CSV safe
        String safeContent = content.replace(",", ";");
        MessageManager.getInstance().sendMessage(student.getUsername(), to, safeContent);
        sendJson(ex, 200, "{\"success\":true}");
    }

    private void handleContacts(HttpExchange ex, Student student) throws IOException {
        // Build unread-count map: sender → count for current user's unread messages
        List<Message> unread = MessageManager.getInstance().getUnreadForUser(student.getUsername());
        Map<String, Integer> unreadMap = new HashMap<>();
        for (Message m : unread) {
            unreadMap.merge(m.getSender(), 1, Integer::sum);
        }

        List<Student> active = UserManager.getInstance().getAllUsers();
        List<String> contactJsons = new ArrayList<>();
        for (Student s : active) {
            if (!s.getUsername().equalsIgnoreCase(student.getUsername())) {
                int count = unreadMap.getOrDefault(s.getUsername(), 0);
                contactJsons.add("{" +
                    "\"username\":\"" + JsonUtil.escape(s.getUsername()) + "\"," +
                    "\"major\":\"" + JsonUtil.escape(s.getMajor()) + "\"," +
                    "\"unreadCount\":" + count +
                "}");
            }
        }
        sendJson(ex, 200, "{\"contacts\":" + JsonUtil.jsonArray(contactJsons) + "}");
    }
}
