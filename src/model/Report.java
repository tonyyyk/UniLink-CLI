package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Value object representing a user report (complaint).
 * CSV format: id,reporter,reported,reason,timestamp,status
 */
public class Report {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final int    id;
    private final String reporter;
    private final String reported;
    private final String reason;
    private final LocalDateTime timestamp;
    private String status; // PENDING | DISMISSED

    public Report(int id, String reporter, String reported,
                  String reason, LocalDateTime timestamp, String status) {
        this.id        = id;
        this.reporter  = reporter;
        this.reported  = reported;
        this.reason    = reason;
        this.timestamp = timestamp;
        this.status    = status;
    }

    public String toCsvRow() {
        // Sanitise reason: replace commas and newlines so CSV stays clean
        String safeReason = reason.replace(",", ";").replace("\n", " ").replace("\r", "");
        return id + "," + reporter + "," + reported + "," +
               safeReason + "," + timestamp.format(FORMATTER) + "," + status;
    }

    public static Report fromCsvRow(String line) {
        // id,reporter,reported,reason,timestamp,status  (split on first 5 commas)
        String[] p = line.split(",", 6);
        int    id        = Integer.parseInt(p[0].trim());
        String reporter  = p[1].trim();
        String reported  = p[2].trim();
        String reason    = p[3].trim();
        LocalDateTime ts = LocalDateTime.parse(p[4].trim(), FORMATTER);
        String status    = p[5].trim();
        return new Report(id, reporter, reported, reason, ts, status);
    }

    public int    getId()        { return id; }
    public String getReporter()  { return reporter; }
    public String getReported()  { return reported; }
    public String getReason()    { return reason; }
    public String getStatus()    { return status; }
    public void   setStatus(String s) { this.status = s; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
