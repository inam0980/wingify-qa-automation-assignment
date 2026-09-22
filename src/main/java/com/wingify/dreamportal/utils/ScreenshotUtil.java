package com.wingify.dreamportal.utils;

import com.aventstack.extentreports.ExtentTest;
import com.wingify.dreamportal.config.ConfigReader;
import com.wingify.dreamportal.driver.DriverFactory;
import com.wingify.dreamportal.listeners.ExtentManager;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Captures PNG screenshots to disk and attaches them to both reports.
 *
 * <p>Every method here is best-effort: a screenshot is diagnostic output, so a failure to capture
 * one is logged and swallowed rather than allowed to mask the real test failure.
 */
public final class ScreenshotUtil {

    private static final Logger LOG = LogManager.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);

    private ScreenshotUtil() {
    }

    /**
     * Saves a screenshot of the current browser view.
     *
     * @param label short name for the shot; sanitised into the file name
     * @return the written file, or empty when no driver was available or the capture failed
     */
    public static Optional<Path> capture(String label) {
        if (!DriverFactory.hasDriver()) {
            LOG.debug("Skipping screenshot '{}' - no driver on this thread", label);
            return Optional.empty();
        }
        return capture(DriverFactory.getDriver(), label);
    }

    /** @see #capture(String) */
    public static Optional<Path> capture(WebDriver driver, String label) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path directory = screenshotDirectory();
            Files.createDirectories(directory);

            Path file = directory.resolve(
                    "%s_%s.png".formatted(sanitise(label), LocalDateTime.now().format(TIMESTAMP)));
            Files.write(file, png);
            LOG.info("Screenshot saved: {}", file.toAbsolutePath());

            Allure.addAttachment(label, "image/png", new ByteArrayInputStream(png), ".png");
            return Optional.of(file);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not capture screenshot '{}': {}", label, e.toString());
            return Optional.empty();
        }
    }

    /**
     * Captures once and puts the image everywhere it is useful: a PNG on disk, an attachment on
     * the Allure step, and a base64 image embedded in the Extent HTML so that report stays a
     * single portable file.
     *
     * <p>Must be called while the browser is still open. TestNG runs {@code ITestListener}
     * callbacks after {@code @AfterMethod}, so capturing from the listener would always find the
     * driver already quit — which is why {@code BaseTest} calls this instead.
     *
     * @param label short name for the shot
     */
    public static void captureAndAttach(String label) {
        if (!DriverFactory.hasDriver()) {
            LOG.debug("Skipping screenshot '{}' - no driver on this thread", label);
            return;
        }

        capture(label);

        ExtentTest test = ExtentManager.getCurrentTest();
        if (test == null) {
            return;
        }
        captureAsBase64(label).ifPresent(base64 -> {
            try {
                test.addScreenCaptureFromBase64String(base64, label);
            } catch (RuntimeException e) {
                LOG.warn("Could not embed the screenshot in the Extent report: {}", e.getMessage());
            }
        });
    }

    /**
     * @return the current view as a base64 string for embedding directly into the Extent HTML
     *     report, or empty when the capture failed
     */
    public static Optional<String> captureAsBase64(String label) {
        if (!DriverFactory.hasDriver()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    ((TakesScreenshot) DriverFactory.getDriver()).getScreenshotAs(OutputType.BASE64));
        } catch (RuntimeException e) {
            LOG.warn("Could not capture base64 screenshot '{}': {}", label, e.toString());
            return Optional.empty();
        }
    }

    public static Path screenshotDirectory() {
        return Path.of(ConfigReader.get("screenshot.dir", "target/screenshots"));
    }

    private static String sanitise(String label) {
        String cleaned = label.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        return cleaned.isEmpty() ? "screenshot" : cleaned;
    }
}
