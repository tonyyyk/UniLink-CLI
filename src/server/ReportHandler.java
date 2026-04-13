package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.ReportManager;
import model.Report;
import model.Student;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles report endpoints:
 *   POST /api/reports/submit   — any authenticated user submits a report
 *   GET  /api/reports          — admin only: view all reports
 *   POST /api/reports/dismiss  — admin only: dismiss a report by id
 */
public class ReportHandler extends BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            String path = ex.getRequestURI().getPath();

            if (path.endsWith("/submit")) {
                handleSubmit(ex, student);
            } else if (path.endsWith("/dismiss")) {
                handleDismiss(ex, student);
            } else {
                handleList(ex, student);
            }
        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }

    /** POST /api/reports/submit — {reported, reason} */
    private void handleSubmit(HttpExchange ex, Student student) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) {
            sendError(ex, 405, "POST required"); return;
        }
        String body     = readBody(ex);
        String reported = JsonUtil.parseString(body, "reported");
        String reason   = JsonUtil.parseString(body, "reason");

        if (reported == null || reported.isBlank()) {
            sendError(ex, 400, "reported is required"); return;
        }
        if (reason == null || reason.isBlank()) {
            sendError(ex, 400, "reason is required"); return;
        }
        if (reported.equalsIgnoreCase(student.getUsername())) {
            sendError(ex, 400, "You cannot report yourself"); return;
        }

        ReportManager.getInstance().submitReport(student.getUsername(), reported, reason);
        sendJson(ex, 200, "{\"success\":true}");
    }

    /** GET /api/reports — admin only, returns all reports */
    private void handleList(HttpExchange ex, Student student) throws IOException {
        if (!student.isAdmin()) {
            sendError(ex, 403, "Admin access required"); return;
        }
        List<Report> reports = ReportManager.getInstance().getAllReports();
        List<String> items   = new ArrayList<>();
        for (Report r : reports) {
            items.add("{" +
                "\"id\":"         + r.getId()                         + "," +
                "\"reporter\":\"" + JsonUtil.escape(r.getReporter())  + "\"," +
                "\"reported\":\"" + JsonUtil.escape(r.getReported())  + "\"," +
                "\"reason\":\""   + JsonUtil.escape(r.getReason())    + "\"," +
                "\"timestamp\":\"" + JsonUtil.escape(r.getFormattedTimestamp()) + "\"," +
                "\"status\":\""   + JsonUtil.escape(r.getStatus())    + "\"" +
            "}");
        }
        sendJson(ex, 200, "{\"reports\":" + JsonUtil.jsonArray(items) + "}");
    }

    /** POST /api/reports/dismiss — admin only, {id} */
    private void handleDismiss(HttpExchange ex, Student student) throws IOException {
        if (!student.isAdmin()) {
            sendError(ex, 403, "Admin access required"); return;
        }
        if (!ex.getRequestMethod().equals("POST")) {
            sendError(ex, 405, "POST required"); return;
        }
        String body = readBody(ex);
        int id = JsonUtil.parseInt(body, "id");
        if (id < 0) {
            sendError(ex, 400, "id is required"); return;
        }
        ReportManager.getInstance().dismissReport(id);
        sendJson(ex, 200, "{\"success\":true}");
    }
}
