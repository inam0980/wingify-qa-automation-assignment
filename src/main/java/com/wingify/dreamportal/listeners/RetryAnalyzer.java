package com.wingify.dreamportal.listeners;

import com.wingify.dreamportal.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed test a bounded number of times.
 *
 * <p>This exists for genuinely environmental failures — a GitHub Pages request that times out, a
 * rate-limited AI call — not to hide a real defect: a broken assertion fails identically on every
 * attempt, so a retried failure is still a reported failure. Each retry is logged so the report
 * shows that it happened.
 *
 * <p>Set {@code retry.count=0} to switch retries off entirely.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LogManager.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRIES = ConfigReader.getInt("retry.count", 1);

    private int attemptsSoFar = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attemptsSoFar >= MAX_RETRIES) {
            return false;
        }
        attemptsSoFar++;
        LOG.warn("Retrying '{}' ({} of {}) after: {}",
                result.getMethod().getMethodName(),
                attemptsSoFar,
                MAX_RETRIES,
                result.getThrowable() == null ? "no exception" : result.getThrowable().getMessage());
        return true;
    }
}
