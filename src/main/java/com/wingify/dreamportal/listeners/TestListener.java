package com.wingify.dreamportal.listeners;

import com.aventstack.extentreports.ExtentTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Bridges TestNG's lifecycle into the reports: opens a report node per test, records the outcome,
 * and flushes the Extent report when the suite ends.
 *
 * <p>Registered in {@code testng.xml} rather than with {@code @Listeners}, so every suite picks it
 * up without each test class having to remember an annotation.
 *
 * <p>Screenshots are captured by {@code BaseTest}, not here: TestNG runs these result callbacks
 * after {@code @AfterMethod}, by which point the browser has already been closed.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger LOG = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ISuite suite) {
        LOG.info("================ SUITE START: {} ================", suite.getName());
        ExtentManager.getInstance();
    }

    @Override
    public void onStart(ITestContext context) {
        LOG.info("---- Test context start: {} ----", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = ExtentManager.getInstance()
                .createTest(displayName(result), description(result));
        test.assignCategory(result.getTestClass().getRealClass().getSimpleName());
        ExtentManager.setCurrentTest(test);
        LOG.info(">>>> START: {}", displayName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("<<<< PASS : {} ({} ms)", displayName(result), durationMillis(result));
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.pass("Test passed in " + durationMillis(result) + " ms");
        }
        ExtentManager.clearCurrentTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable error = result.getThrowable();
        LOG.error("<<<< FAIL : {} ({} ms)", displayName(result), durationMillis(result), error);

        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            if (error == null) {
                test.fail("Test failed without an exception");
            } else {
                test.fail(error);
            }
        }
        // The screenshot is attached by BaseTest's @AfterMethod, which runs before this callback
        // and is the last point at which the browser is still open.
        ExtentManager.clearCurrentTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Throwable cause = result.getThrowable();
        String reason = cause == null ? "no reason recorded" : cause.getMessage();
        LOG.warn("<<<< SKIP : {} - {}", displayName(result), reason);

        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.skip("Skipped: " + reason);
        }
        ExtentManager.clearCurrentTest();
    }

    @Override
    public void onFinish(ITestContext context) {
        LOG.info("---- Test context end: {} | passed={} failed={} skipped={} ----",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
        LOG.info("================ SUITE END: {} ================", suite.getName());
    }

    private static String displayName(ITestResult result) {
        Object[] parameters = result.getParameters();
        String methodName = result.getMethod().getMethodName();
        if (parameters == null || parameters.length == 0) {
            return methodName;
        }
        // Data-driven tests get the parameter in the name, so the AI test reads as
        // "aiClassificationMatchesTheDiary[Monster chase]" instead of ten identical rows.
        return methodName + "[" + parameters[0] + "]";
    }

    private static String description(ITestResult result) {
        String description = result.getMethod().getDescription();
        return description == null ? "" : description;
    }

    private static long durationMillis(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
