package com.wingify.dreamportal.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.wingify.dreamportal.config.ConfigReader;
import java.io.File;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Owns the single {@link ExtentReports} instance and the per-thread {@link ExtentTest} node.
 *
 * <p>Extent produces one self-contained HTML file, which is what a reviewer opens without
 * installing anything. Allure runs alongside it for the richer, step-level view.
 */
public final class ExtentManager {

    private static final Logger LOG = LogManager.getLogger(ExtentManager.class);
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();

    private static ExtentReports extent;

    private ExtentManager() {
    }

    /** Creates the report on first use. Safe to call from every listener callback. */
    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            extent = build();
        }
        return extent;
    }

    private static ExtentReports build() {
        Path reportPath = reportPath();
        //noinspection ResultOfMethodCallIgnored
        reportPath.toFile().getParentFile().mkdirs();

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath.toFile());
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("Dream Portal - Automation Report");
        spark.config().setReportName("Dream Portal QA Automation");
        spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(spark);
        reports.setSystemInfo("Application", ConfigReader.get("base.url", "n/a"));
        reports.setSystemInfo("Browser", ConfigReader.get("browser", "chrome"));
        reports.setSystemInfo("Headless", String.valueOf(ConfigReader.getBoolean("headless", true)));
        reports.setSystemInfo("AI provider", ConfigReader.get("ai.provider", "rule"));
        reports.setSystemInfo("Java", System.getProperty("java.version"));
        reports.setSystemInfo("OS", System.getProperty("os.name"));

        LOG.info("Extent report will be written to {}", reportPath.toAbsolutePath());
        return reports;
    }

    public static Path reportPath() {
        return Path.of(ConfigReader.get("extent.report.path", "target/reports/ExtentReport.html"));
    }

    static void setCurrentTest(ExtentTest test) {
        CURRENT_TEST.set(test);
    }

    static void clearCurrentTest() {
        CURRENT_TEST.remove();
    }

    /** @return the report node for the running test, or {@code null} outside a test. */
    public static ExtentTest getCurrentTest() {
        return CURRENT_TEST.get();
    }

    /** Writes everything buffered so far to disk. */
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            File file = reportPath().toFile();
            LOG.info("Extent report written: {} ({} KB)",
                    file.getAbsolutePath(), Math.max(1, file.length() / 1024));
        }
    }
}
