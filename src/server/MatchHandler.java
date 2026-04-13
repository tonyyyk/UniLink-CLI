package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.UserManager;
import model.Student;
import strategy.ComplementaryMatchingStrategy;
import strategy.MatchingStrategy;
import strategy.SameMajorMatchingStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handles partner matching:
 *   GET /api/match?strategy=complementary  (default)
 *   GET /api/match?strategy=samemajor
 */
public class MatchHandler extends BaseHandler implements HttpHandler {

    private static final MatchingStrategy COMPLEMENTARY = new ComplementaryMatchingStrategy();
    private static final MatchingStrategy SAME_MAJOR    = new SameMajorMatchingStrategy();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("OPTIONS")) { handleOptions(ex); return; }

        try {
            Student student = authenticate(ex);
            if (student == null) return;

            String strategyParam = getQueryParam(ex, "strategy");
            MatchingStrategy strategy = "samemajor".equalsIgnoreCase(strategyParam)
                    ? SAME_MAJOR : COMPLEMENTARY;

            List<Student> candidates = UserManager.getInstance().getAllActiveStudents();

            // Score and sort (exclude self)
            List<double[]> scored = new ArrayList<>();  // [score, index]
            List<Student> eligible = new ArrayList<>();
            for (Student candidate : candidates) {
                if (candidate.getUsername().equalsIgnoreCase(student.getUsername())) continue;
                double score = strategy.calculateScore(student, candidate);
                scored.add(new double[]{score, eligible.size()});
                eligible.add(candidate);
            }
            scored.sort((a, b) -> Double.compare(b[0], a[0]));

            List<String> results = new ArrayList<>();
            for (double[] entry : scored) {
                int idx = (int) entry[1];
                double score = entry[0];
                Student c = eligible.get(idx);
                results.add("{" +
                    "\"username\":\"" + JsonUtil.escape(c.getUsername()) + "\"," +
                    "\"major\":\"" + JsonUtil.escape(c.getMajor()) + "\"," +
                    "\"score\":" + (int) Math.round(score * 100) + "," +
                    "\"strengths\":" + JsonUtil.toJsonStringArray(c.getStrengths()) + "," +
                    "\"weaknesses\":" + JsonUtil.toJsonStringArray(c.getWeaknesses()) +
                "}");
            }

            sendJson(ex, 200, "{" +
                "\"strategy\":\"" + JsonUtil.escape(strategy.getStrategyName()) + "\"," +
                "\"results\":" + JsonUtil.jsonArray(results) +
            "}");

        } catch (Exception e) {
            sendError(ex, 500, "Internal server error: " + e.getMessage());
        }
    }
}
