package com.wingify.dreamportal.driver;

import com.wingify.dreamportal.config.ConfigReader;
import java.time.Duration;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Creates and hands out the {@link WebDriver} for the current thread.
 *
 * <p>The driver lives in a {@link ThreadLocal} so the suite stays correct if TestNG is ever run
 * with parallel methods — each thread gets its own browser and its own teardown.
 *
 * <p>Binaries are resolved by Selenium Manager (built into Selenium 4.6+), so no driver
 * executables are committed and no WebDriverManager dependency is needed.
 */
public final class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    /**
     * Starts a browser for the current thread using the configured {@code browser} and
     * {@code headless} values.
     *
     * @return the freshly created driver, also retrievable via {@link #getDriver()}
     */
    public static WebDriver createDriver() {
        if (DRIVER.get() != null) {
            LOG.warn("A driver already exists on this thread; quitting it before creating a new one");
            quitDriver();
        }

        String browser = ConfigReader.get("browser", "chrome").toLowerCase(Locale.ROOT);
        boolean headless = ConfigReader.getBoolean("headless", true);
        LOG.info("Starting browser '{}' (headless={})", browser, headless);

        WebDriver driver = switch (browser) {
            case "chrome" -> new ChromeDriver(chromeOptions(headless));
            case "edge" -> new EdgeDriver(edgeOptions(headless));
            case "firefox" -> new FirefoxDriver(firefoxOptions(headless));
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '" + browser + "'. Use chrome, edge or firefox.");
        };

        // The suite waits explicitly everywhere; an implicit wait on top of that makes
        // "element should be absent" checks slow and the two wait types interact badly.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigReader.getInt("page.load.timeout.seconds", 30)));

        DRIVER.set(driver);
        return driver;
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=1920,1080",
                // The portal opens two tabs from a single click; without this the second
                // window.open() can be swallowed by the popup blocker.
                "--disable-popup-blocking",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--remote-allow-origins=*");
        options.setExperimentalOption("excludeSwitches", new String[] {"enable-automation"});
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=1920,1080",
                "--disable-popup-blocking",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage");
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=1920", "--height=1080");
        // Firefox blocks scripted window.open() by default, which would break the two-tab test.
        options.addPreference("dom.disable_open_during_load", false);
        return options;
    }

    /**
     * @return the driver bound to the current thread
     * @throws IllegalStateException when called before {@link #createDriver()}
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No WebDriver on this thread. Did the test extend BaseTest?");
        }
        return driver;
    }

    /** @return {@code true} when a driver is currently bound to this thread. */
    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    /** Quits the driver and clears the thread binding. Safe to call when no driver exists. */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
            LOG.info("Browser closed");
        } catch (RuntimeException e) {
            LOG.warn("Ignoring error while quitting the browser: {}", e.getMessage());
        } finally {
            DRIVER.remove();
        }
    }
}
