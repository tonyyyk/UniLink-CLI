package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.UserManager;
import model.Student;

import java.io.IOException;
import java.util.List;

/**
 * Handles profile endpoints:
 *   GET /api/profile   — return the current user's profile
 *   PUT /api/profile   — update major, strengths, weaknesses
 */
public class ProfileHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            if (method.equals("GET")) {
                sendJson(ex, 200, JsonUtil.toJson(student));
            } else if (method.equals("PUT")) {
                handleUpdate(ex, student);
            } else {
                sendError(ex, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleUpdate(HttpExchange ex, Student student) throws IOException {
        if (!student.canUpdateProfile()) {
            sendError(ex, 403, "Your account status does not permit profile updates");
            return;
        }

        String body = readBody(ex);

        // Only update fields that are present in the request body
        String major = JsonUtil.parseString(body, "major");
        if (major != null && !major.isBlank()) {
            student.setMajor(major);
        }

        if (body.contains("\"strengths\"")) {
            List<String> strengths = JsonUtil.parseStringArray(body, "strengths");
            student.setStrengths(strengths);
        }

        if (body.contains("\"weaknesses\"")) {
            List<String> weaknesses = JsonUtil.parseStringArray(body, "weaknesses");
            student.setWeaknesses(weaknesses);
        }

        String dob = JsonUtil.parseString(body, "dateOfBirth");
        if (dob != null) student.setDateOfBirth(dob);

        String gender = JsonUtil.parseString(body, "gender");
        if (gender != null) student.setGender(gender);

        if (body.contains("\"hobbies\"")) {
            List<String> hobbies = JsonUtil.parseStringArray(body, "hobbies");
            student.setHobbies(hobbies);
        }

        String intro = JsonUtil.parseString(body, "introduction");
        if (intro != null) student.setIntroduction(intro);

        UserManager.getInstance().updateProfile(student);
        sendJson(ex, 200, "{\"success\":true}");
    }
}
