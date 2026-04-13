package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.GroupManager;
import model.Student;
import model.StudyGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles study group endpoints:
 *   GET  /api/groups           — all groups
 *   GET  /api/groups/mine      — groups the current user belongs to
 *   POST /api/groups/create    — create a new group
 *   POST /api/groups/join      — join an existing group
 */
public class GroupHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            String path = ex.getRequestURI().getPath();

            if (path.endsWith("/mine") && method.equals("GET")) {
                handleMine(ex, student);
            } else if (path.endsWith("/create") && method.equals("POST")) {
                handleCreate(ex, student);
            } else if (path.endsWith("/join") && method.equals("POST")) {
                handleJoin(ex, student);
            } else if (path.equals("/api/groups") || path.equals("/api/groups/")) {
                handleAll(ex, student);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleAll(HttpExchange ex, Student student) throws IOException {
        List<StudyGroup> groups = GroupManager.getInstance().getAllGroups();
        sendJson(ex, 200, "{\"groups\":" + groupListToJson(groups, student.getUsername()) + "}");
    }

    private void handleMine(HttpExchange ex, Student student) throws IOException {
        List<StudyGroup> groups = GroupManager.getInstance().getGroupsForUser(student.getUsername());
        sendJson(ex, 200, "{\"groups\":" + groupListToJson(groups, student.getUsername()) + "}");
    }

    private void handleCreate(HttpExchange ex, Student student) throws IOException {
        String body = readBody(ex);
        String name  = JsonUtil.parseString(body, "name");
        String topic = JsonUtil.parseString(body, "topic");

        if (name == null || name.isBlank()) {
            sendError(ex, 400, "name is required");
            return;
        }
        if (topic == null) topic = "";

        int groupId = GroupManager.getInstance().createGroup(name, student.getUsername(), topic);
        sendJson(ex, 200, "{\"success\":true,\"groupId\":" + groupId + "}");
    }

    private void handleJoin(HttpExchange ex, Student student) throws IOException {
        String body = readBody(ex);
        int groupId = JsonUtil.parseInt(body, "groupId");

        if (groupId < 0) {
            sendError(ex, 400, "groupId is required");
            return;
        }

        boolean success = GroupManager.getInstance().joinGroup(groupId, student.getUsername());
        if (!success) {
            sendError(ex, 404, "Group not found");
            return;
        }
        sendJson(ex, 200, "{\"success\":true}");
    }

    private String groupListToJson(List<StudyGroup> groups, String currentUser) {
        List<String> items = new ArrayList<>();
        for (StudyGroup g : groups) items.add(JsonUtil.toJson(g, currentUser));
        return JsonUtil.jsonArray(items);
    }
}
