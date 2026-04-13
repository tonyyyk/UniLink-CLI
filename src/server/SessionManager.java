package server;

import model.Student;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages web session tokens for the HTTP server.
 * Maps UUID token strings to authenticated Student objects.
 *
 * Singleton — one map for the lifetime of the server.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    private final ConcurrentHashMap<String, Student> sessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Create a new session for the given student and return the token. */
    public String createSession(Student student) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, student);
        return token;
    }

    /** Return the Student for the given token, or null if not found. */
    public Student getStudent(String token) {
        return token == null ? null : sessions.get(token);
    }

    /** Remove the session associated with this token (on logout). */
    public void removeSession(String token) {
        if (token != null) sessions.remove(token);
    }

    public boolean isLoggedIn(String token) {
        return token != null && sessions.containsKey(token);
    }
}
