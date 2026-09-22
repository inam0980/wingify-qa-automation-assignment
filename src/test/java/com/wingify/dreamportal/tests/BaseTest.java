package com.wingify.dreamportal.tests;

import com.wingify.dreamportal.config.ConfigReader;
import com.wingify.dreamportal.driver.DriverFactory;
import com.wingify.dreamportal.pages.DreamsDiaryPage;
import com.wingify.dreamportal.pages.DreamsTotalPage;
import com.wingify.dreamportal.pages.HomePage;
import com.wingify.dreamportal.utils.ScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Lifecycle shared by every UI test: one fresh browser per test method, always closed.
 *
 * <p>A browser per method rather than per class keeps the tests independent — no test can leave
 * state (an extra tab, a scrolled page) that changes the outcome of the next one.
 */
public abstract class BaseTest {

    private static final Logger LOG = LogManager.getLogger(BaseTest.class);

    protected WebDriver driver;

    @BeforeSuite(alwaysRun = true)
    public void logRunConfiguration() {
        LOG.info("Application under test : {}", ConfigReader.getRequired("base.url"));
        LOG.info("Browser                : {} (headless={})",
                ConfigReader.get("browser", "chrome"),
                ConfigReader.getBoolean("headless", true));
        LOG.info("AI provider            : {}", ConfigReader.get("ai.provider", "rule"));
        LOG.info("Java                   : {}", System.getProperty("java.version"));
    }

    @BeforeMethod(alwaysRun = true)
    public void startBrowser() {
        driver = DriverFactory.createDriver();
    }

    /**
     * Captures the final state of the page, then closes the browser.
     *
     * <p>The screenshot is taken here rather than in {@code TestListener} because TestNG runs its
     * {@code ITestListener} result callbacks <em>after</em> {@code @AfterMethod} — by then the
     * driver is gone and there is nothing left to photograph.
     */
    @AfterMethod(alwaysRun = true)
    public void captureAndCloseBrowser(ITestResult result) {
        boolean failed = result.getStatus() == ITestResult.FAILURE;
        if (failed || ConfigReader.getBoolean("screenshot.on.success", false)) {
            ScreenshotUtil.captureAndAttach(
                    result.getMethod().getMethodName() + "_" + (failed ? "failed" : "passed"));
        }
        DriverFactory.quitDriver();
        driver = null;
    }

    protected HomePage homePage() {
        return new HomePage(driver);
    }

    protected DreamsDiaryPage diaryPage() {
        return new DreamsDiaryPage(driver);
    }

    protected DreamsTotalPage totalPage() {
        return new DreamsTotalPage(driver);
    }
}
