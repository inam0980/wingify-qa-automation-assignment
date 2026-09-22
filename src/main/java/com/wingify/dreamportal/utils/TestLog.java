package com.wingify.dreamportal.utils;

import com.aventstack.extentreports.ExtentTest;
import com.wingify.dreamportal.listeners.ExtentManager;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * One call that records a line in all three places a reviewer might look: the console/file log,
 * the Extent HTML report, and the Allure timeline.
 *
 * <p>Without this the three outputs drift apart and the HTML report ends up with nothing in it
 * but pass/fail.
 */
public final class TestLog {

    private static final Logger LOG = LogManager.getLogger(TestLog.class);

    private TestLog() {
    }

    /** Records a normal progress line. */
    public static void info(String message) {
        LOG.info(message);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.info(message);
        }
        Allure.step(message);
    }

    /** Formats with {@link String#formatted} then records, e.g. {@code info("Found %d rows", n)}. */
    public static void info(String template, Object... args) {
        info(template.formatted(args));
    }

    /** Records a line that represents a satisfied expectation. */
    public static void pass(String message) {
        LOG.info("PASS - {}", message);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.pass(message);
        }
        Allure.step(message);
    }

    public static void pass(String template, Object... args) {
        pass(template.formatted(args));
    }

    /** Records a non-fatal warning; the test carries on. */
    public static void warn(String message) {
        LOG.warn(message);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.warning(message);
        }
    }

    public static void warn(String template, Object... args) {
        warn(template.formatted(args));
    }

    /** Attaches free-form text (a table dump, a raw API response) to both reports. */
    public static void attachText(String name, String content) {
        LOG.info("{}:{}{}", name, System.lineSeparator(), content);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.info("<b>" + name + "</b><pre>" + escapeHtml(content) + "</pre>");
        }
        Allure.addAttachment(name, "text/plain", content, ".txt");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
