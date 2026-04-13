package manager;

import model.Report;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SINGLETON PATTERN — ReportManager.
 * Manages the reports.csv file: submit, list, and dismiss reports.
 *
 * CSV format: id,reporter,reported,reason,timestamp,status
 */
public class ReportManager {

    private static ReportManager instance;
    static final String FILE_PATH  = "data/reports.csv";
    private static final String CSV_HEADER = "id,reporter,reported,reason,timestamp,status";

    private ReportManager() {}

    public static ReportManager getInstance() {
        if (instance == null) {
            instance = new ReportManager();
        }
        return instance;
    }

    public void initialise() throws IOException {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(CSV_HEADER);
                bw.newLine();
            }
        }
    }

    /** Submit a new PENDING report. */
    public void submitReport(String reporter, String reported, String reason)
            throws IOException {
        int id = getNextId();
        Report r = new Report(id, reporter, reported, reason, LocalDateTime.now(), "PENDING");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(r.toCsvRow());
            bw.newLine();
        }
    }

    /** Return all reports (all statuses), most recent first. */
    public List<Report> getAllReports() throws IOException {
        List<Report> reports = new ArrayList<>();
        for (String line : readRawLines()) {
            if (line.trim().isEmpty() || line.startsWith("id")) continue;
            reports.add(Report.fromCsvRow(line));
        }
        // Reverse so newest appears first
        java.util.Collections.reverse(reports);
        return reports;
    }

    /** Set a report's status to DISMISSED. */
    public void dismissReport(int id) throws IOException {
        List<String> lines   = readRawLines();
        List<String> updated = new ArrayList<>();
        updated.add(CSV_HEADER);
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("id")) continue;
            Report r = Report.fromCsvRow(line);
            if (r.getId() == id) r.setStatus("DISMISSED");
            updated.add(r.toCsvRow());
        }
        writeLines(updated);
    }

    private int getNextId() throws IOException {
        int max = 0;
        for (String line : readRawLines()) {
            if (line.trim().isEmpty() || line.startsWith("id")) continue;
            try {
                int id = Integer.parseInt(line.split(",")[0].trim());
                if (id > max) max = id;
            } catch (NumberFormatException ignored) {}
        }
        return max + 1;
    }

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

    private void writeLines(List<String> lines) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }
}
