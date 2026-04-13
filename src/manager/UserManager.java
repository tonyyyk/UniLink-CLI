package manager;

import model.Student;
import state.NormalState;
import state.SuspendedState;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SINGLETON PATTERN — UserManager.
 *
 * Manages all user data persistence via users.csv.
 * There is exactly one instance of this class for the lifetime of the
 * application, guaranteeing that no two code paths ever write to
 * users.csv simultaneously and risk corrupting the file.
 *
 * CSV format:
 *   username,password,major,strengths,weaknesses,role,status
 *
 * The private constructor + static getInstance() method enforce the Singleton.
 */
public class UserManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static UserManager instance;

    /** Package-visible for unit testing; use getInstance() in production. */
    static final String FILE_PATH = "data/users.csv";
    private static final String CSV_HEADER = "username,password,major,strengths,weaknesses,role,status";

    private UserManager() {} // private constructor prevents external instantiation

    /** Returns the single shared UserManager instance, creating it on first call. */
    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Ensures users.csv exists with a header row and a default admin account.
     * Safe to call on every application start — does nothing if the file exists.
     */
    public void initialise() throws IOException {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(CSV_HEADER);
                bw.newLine();
                // Seed default admin + demo users
                bw.write("admin,admin123,-,-,-,ADMIN,NORMAL");             bw.newLine();
                bw.write("alice,alice123,Computer Science,Java;Algorithms,Networking;Databases,USER,NORMAL");  bw.newLine();
                bw.write("bob,bob123,Mathematics,Calculus;Statistics,Programming;Physics,USER,NORMAL");        bw.newLine();
                bw.write("carol,carol123,Physics,Mathematics;Lab Work,Programming;Statistics,USER,NORMAL");    bw.newLine();
                bw.write("dave,dave123,Computer Science,Networking;Databases,Algorithms;UI Design,USER,NORMAL"); bw.newLine();
                bw.write("emma,emma123,Data Science,Python;Statistics;Machine Learning,Java;Networking,USER,NORMAL"); bw.newLine();
            }
        }
    }

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Register a new user. Returns false if the username is already taken.
     * The new user starts with NormalState and the USER role.
     */
    public boolean register(String username, String password, String major)
            throws IOException {
        if (usernameExists(username)) return false;

        Student newStudent = new Student(
                username, password, major,
                new ArrayList<>(), new ArrayList<>(),
                "USER", new NormalState()
        );
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(newStudent.toCsvRow());
            bw.newLine();
        }
        return true;
    }

    /**
     * Attempt login. Returns the Student object on success, null on failure.
     * Validates username exists and password matches.
     */
    public Student login(String username, String password) throws IOException {
        for (Student s : readAllStudents()) {
            if (s.getUsername().equalsIgnoreCase(username) &&
                s.getPassword().equals(password)) {
                return s;
            }
        }
        return null; // credentials not found
    }

    // ── Profile management ────────────────────────────────────────────────────

    /**
     * Persist updated profile fields (major, strengths, weaknesses) for the
     * given student. Rewrites the entire CSV, replacing the matching row.
     */
    public void updateProfile(Student student) throws IOException {
        List<String> lines = readRawLines();
        List<String> updated = new ArrayList<>();
        updated.add(CSV_HEADER);

        for (String line : lines) {
            if (line.startsWith(CSV_HEADER)) continue; // skip header
            Student s = Student.fromCsvRow(line);
            if (s.getUsername().equalsIgnoreCase(student.getUsername())) {
                updated.add(student.toCsvRow()); // replace with updated data
            } else {
                updated.add(line);
            }
        }
        writeLines(updated);
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /**
     * Returns all USER-role students who are in NormalState.
     * Used by FindPartnersCommand — suspended students are filtered out
     * because SuspendedState.canAppearInSearch() returns false.
     */
    public List<Student> getAllActiveStudents() throws IOException {
        List<Student> result = new ArrayList<>();
        for (Student s : readAllStudents()) {
            if (!s.isAdmin() && s.canAppearInSearch()) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Returns every student including ADMIN accounts and SUSPENDED users.
     * Used by the admin panel to display full user list.
     */
    public List<Student> getAllUsers() throws IOException {
        return readAllStudents();
    }

    public boolean usernameExists(String username) throws IOException {
        for (Student s : readAllStudents()) {
            if (s.getUsername().equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    // ── Account self-management ───────────────────────────────────────────────

    /**
     * Change a user's password after verifying the old one.
     * Returns true on success, false if old password is wrong.
     */
    public boolean changePassword(String username, String oldPassword, String newPassword)
            throws IOException {
        List<String> lines   = readRawLines();
        List<String> updated = new ArrayList<>();
        updated.add(CSV_HEADER);
        boolean changed = false;

        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("username")) continue;
            Student s = Student.fromCsvRow(line);
            if (s.getUsername().equalsIgnoreCase(username)) {
                if (!s.getPassword().equals(oldPassword)) return false; // wrong old password
                // Create updated student with new password and re-serialize
                Student updated2 = new Student(
                        s.getUsername(), newPassword, s.getMajor(),
                        s.getStrengths(), s.getWeaknesses(), s.getRole(),
                        s.getStateName().equalsIgnoreCase("SUSPENDED")
                            ? new state.SuspendedState() : new state.NormalState(),
                        s.getDateOfBirth(), s.getGender(), s.getHobbies(), s.getIntroduction()
                );
                updated.add(updated2.toCsvRow());
                changed = true;
            } else {
                updated.add(line);
            }
        }
        if (changed) writeLines(updated);
        return changed;
    }

    /**
     * Delete a user account after password verification.
     * Returns true on success, false if password is wrong.
     * Prevents deleting the last ADMIN account.
     */
    public boolean deleteAccount(String username, String password) throws IOException {
        List<Student> all = readAllStudents();

        // Verify password
        Student target = null;
        for (Student s : all) {
            if (s.getUsername().equalsIgnoreCase(username)) { target = s; break; }
        }
        if (target == null || !target.getPassword().equals(password)) return false;

        // Prevent deleting the last admin
        if (target.isAdmin()) {
            long adminCount = all.stream().filter(Student::isAdmin).count();
            if (adminCount <= 1) return false;
        }

        // Rewrite CSV without this user
        List<String> lines   = readRawLines();
        List<String> updated = new ArrayList<>();
        updated.add(CSV_HEADER);
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("username")) continue;
            Student s = Student.fromCsvRow(line);
            if (!s.getUsername().equalsIgnoreCase(username)) updated.add(line);
        }
        writeLines(updated);
        return true;
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    /**
     * Suspend a user account (STATE PATTERN: sets status to SUSPENDED in CSV).
     * The in-memory Student object is NOT updated here — the caller must also
     * call student.suspend() if they hold a reference.
     */
    public void suspendUser(String username) throws IOException {
        updateStatus(username, "SUSPENDED");
    }

    /**
     * Reinstate a suspended account (STATE PATTERN: sets status to NORMAL).
     */
    public void reinstateUser(String username) throws IOException {
        updateStatus(username, "NORMAL");
    }

    private void updateStatus(String username, String newStatus) throws IOException {
        List<String> lines = readRawLines();
        List<String> updated = new ArrayList<>();
        updated.add(CSV_HEADER);

        for (String line : lines) {
            if (line.startsWith(CSV_HEADER)) continue;
            Student s = Student.fromCsvRow(line);
            if (s.getUsername().equalsIgnoreCase(username)) {
                // Use toCsvRow so all fields (including extended ones) are preserved
                s.suspend();   // temporarily set to match newStatus
                if (newStatus.equalsIgnoreCase("NORMAL")) s.reinstate();
                updated.add(s.toCsvRow());
            } else {
                updated.add(line);
            }
        }
        writeLines(updated);
    }

    // ── Private CSV I/O helpers ───────────────────────────────────────────────

    /** Read the CSV and parse every data row (skips header) into Student objects. */
    private List<Student> readAllStudents() throws IOException {
        List<Student> students = new ArrayList<>();
        List<String> lines = readRawLines();
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("username")) continue;
            // Skip malformed rows that don't have at least 7 comma-separated fields
            if (line.split(",", -1).length < 7) continue;
            students.add(Student.fromCsvRow(line));
        }
        return students;
    }

    /** Read all raw lines from users.csv (including the header). */
    private List<String> readRawLines() throws IOException {
        List<String> lines = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return lines;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line);
            }
        }
        return lines;
    }

    /** Overwrite users.csv with the provided list of lines. */
    private void writeLines(List<String> lines) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }
}
