package com.expandtesting.utils;

import io.qameta.allure.Allure;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 3.5 — Performance Engineering.
 *
 * Centralised performance data recorder.  Every UI page-load timing and
 * every API response-time measurement is funnelled through this class so
 * that:
 *   (a) a human-readable trend log is written to target/performance-log.csv
 *   (b) each entry is attached to the current Allure scenario as an artifact
 *
 * CSV columns: timestamp, type (UI|API), scenario, metric, value_ms, threshold_ms, pass
 */
public class PerformanceLogger {

    private static final String LOG_PATH = "target/performance-log.csv";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // Write CSV header once per run (only if the file doesn't exist yet)
        if (!Files.exists(Paths.get(LOG_PATH))) {
            try {
                Files.createDirectories(Paths.get("target"));
                try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_PATH, false))) {
                    pw.println("timestamp,type,scenario,metric,value_ms,threshold_ms,pass");
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * Records a UI timing measurement (page load or DOM ready).
     *
     * @param scenarioName  Cucumber scenario name (for the report row)
     * @param metric        e.g. "pageLoad" or "domReady"
     * @param valueMs       measured duration
     * @param thresholdMs   acceptable maximum (e.g. 3000 for UI)
     */
    public static void logUiTiming(String scenarioName, String metric,
                                   long valueMs, long thresholdMs) {
        log("UI", scenarioName, metric, valueMs, thresholdMs);
    }

    /**
     * Records an API response-time measurement.
     *
     * @param scenarioName  Cucumber scenario name
     * @param endpoint      e.g. "POST /notes"
     * @param valueMs       measured response time
     * @param thresholdMs   SLA ceiling — 2 000 ms as per section 2.3
     */
    public static void logApiTiming(String scenarioName, String endpoint,
                                    long valueMs, long thresholdMs) {
        log("API", scenarioName, endpoint, valueMs, thresholdMs);
    }

    // ── internal ──────────────────────────────────────────────────

    private static void log(String type, String scenario, String metric,
                             long valueMs, long thresholdMs) {
        boolean pass  = valueMs >= 0 && valueMs <= thresholdMs;
        String  ts    = LocalDateTime.now().format(FMT);
        String  line  = String.join(",", ts, type, escape(scenario),
                escape(metric), String.valueOf(valueMs),
                String.valueOf(thresholdMs), String.valueOf(pass));

        // 3.5 — write to trend log file
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_PATH, true))) {
            pw.println(line);
        } catch (IOException ignored) {}

        // 3.5 — attach to Allure report as an artifact
        String allureText = String.format(
                "[PERF] %s | %s | %s | %d ms | threshold %d ms | %s",
                type, scenario, metric, valueMs, thresholdMs,
                pass ? "PASS" : "SLOW");
        try {
            Allure.addAttachment("Performance: " + metric,
                    "text/plain", allureText, ".txt");
        } catch (Exception ignored) {
            // Allure context not available outside a scenario — just log to file
        }

        // 3.5 — always print to console so CI logs show timing data
        System.out.println(allureText);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
