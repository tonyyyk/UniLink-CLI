package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.GroupManager;
import manager.GroupMessageManager;
import model.GroupMessage;
import model.Student;
import model.StudyGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles group chat endpoints:
 *   GET  /api/groups/chat?groupId=<id>  — get all messages for a group (members only)
 *   POST /api/groups/chat               — send a message to a group (members only)
 */
public class GroupMessageHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            if (method.equals("GET")) {
                handleGetMessages(ex, student);
            } else if (method.equals("POST")) {
                handleSendMessage(ex, student);
            } else {
                sendError(ex, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetMessages(HttpExchange ex, Student student) throws IOException {
        String groupIdParam = getQueryParam(ex, "groupId");
        if (groupIdParam == null) {
            sendError(ex, 400, "groupId is required");
            return;
        }
        int groupId;
        try {
            groupId = Integer.parseInt(groupIdParam);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "groupId must be a number");
            return;
        }

        // Membership check
        if (!isMember(groupId, student.getUsername())) {
            sendError(ex, 403, "You are not a member of this group");
            return;
        }

        List<GroupMessage> messages = GroupMessageManager.getInstance().getMessages(groupId);
        List<String> jsonItems = new ArrayList<>();
        for (GroupMessage m : messages) {
            jsonItems.add("{" +
                "\"sender\":\"" + JsonUtil.escape(m.getSender()) + "\"," +
                "\"timestamp\":\"" + JsonUtil.escape(m.getFormattedTimestamp()) + "\"," +
                "\"content\":\"" + JsonUtil.escape(m.getContent()) + "\"" +
            "}");
        }
        sendJson(ex, 200, "{\"messages\":" + JsonUtil.jsonArray(jsonItems) + "}");
    }

    private void handleSendMessage(HttpExchange ex, Student student) throws IOException {
        String body = readBody(ex);
        int groupId = JsonUtil.parseInt(body, "groupId");
        String content = JsonUtil.parseString(body, "content");

        if (groupId < 0) {
            sendError(ex, 400, "groupId is required");
            return;
        }
        if (content == null || content.isBlank()) {
            sendError(ex, 400, "content is required");
            return;
        }

        // Membership check
        if (!isMember(groupId, student.getUsername())) {
            sendError(ex, 403, "You are not a member of this group");
            return;
        }

        // Replace commas with semicolons to avoid breaking CSV
        content = content.replace(",", ";");

        GroupMessageManager.getInstance().sendMessage(groupId, student.getUsername(), content);
        sendJson(ex, 200, "{\"success\":true}");
    }

    private boolean isMember(int groupId, String username) throws IOException {
        for (StudyGroup g : GroupManager.getInstance().getAllGroups()) {
            if (g.getGroupId() == groupId) {
                return g.hasMember(username);
            }
        }
        return false;
    }
}
