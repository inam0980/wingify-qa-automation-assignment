package com.wingify.dreamportal.utils;

import com.wingify.dreamportal.config.ConfigReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Helpers for the one genuinely fiddly part of this site: the "My Dreams" button calls
 * {@code window.open()} twice, so a single click has to be observed as two new browser tabs.
 *
 * <p>Tabs are identified by URL rather than by opening order, because the order in which a browser
 * reports window handles is not guaranteed to match the order the tabs were opened.
 */
public final class WindowManager {

    private static final Logger LOG = LogManager.getLogger(WindowManager.class);

    private WindowManager() {
    }

    /**
     * Waits until the browser reports exactly {@code expectedCount} windows.
     *
     * @return the handles once the count is reached
     * @throws TimeoutException when the count never arrives, naming what was actually seen
     */
    public static Set<String> waitForWindowCount(WebDriver driver, int expectedCount) {
        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds", 15)));
        try {
            wait.until(ExpectedConditions.numberOfWindowsToBe(expectedCount));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    "Expected %d browser windows but found %d after waiting"
                            .formatted(expectedCount, driver.getWindowHandles().size()), e);
        }
        Set<String> handles = driver.getWindowHandles();
        LOG.info("Browser now reports {} window(s)", handles.size());
        return handles;
    }

    /** @return the handles present now that were not present before, preserving iteration order. */
    public static Set<String> newHandles(Set<String> before, Set<String> after) {
        Set<String> opened = new LinkedHashSet<>(after);
        opened.removeAll(before);
        return opened;
    }

    /**
     * Visits each handle and records the URL and title of the page it shows.
     *
     * <p>Leaves the driver focused on {@code returnTo} so the caller is not left on a random tab.
     *
     * @return one descriptor per handle, in the iteration order of {@code handles}
     */
    public static List<WindowInfo> describe(WebDriver driver, Set<String> handles, String returnTo) {
        List<WindowInfo> described = new ArrayList<>(handles.size());
        for (String handle : handles) {
            driver.switchTo().window(handle);
            WindowInfo info = new WindowInfo(handle, driver.getCurrentUrl(), driver.getTitle());
            LOG.info("Window {} -> {}", info.title(), info.url());
            described.add(info);
        }
        driver.switchTo().window(returnTo);
        return described;
    }

    /**
     * Focuses the first window whose URL ends with {@code pageName}.
     *
     * @throws IllegalStateException when no open window matches, listing the URLs that were open
     */
    public static void switchToPage(WebDriver driver, String pageName) {
        List<String> seen = new ArrayList<>();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            String currentUrl = driver.getCurrentUrl();
            seen.add(currentUrl);
            if (currentUrl.endsWith(pageName)) {
                LOG.info("Switched to the tab showing {}", pageName);
                return;
            }
        }
        throw new IllegalStateException(
                "No open tab is showing '" + pageName + "'. Tabs currently open: " + seen);
    }

    /** Closes every window except {@code keepHandle} and focuses the one kept. */
    public static void closeAllExcept(WebDriver driver, String keepHandle) {
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(keepHandle)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(keepHandle);
        LOG.info("Closed extra tabs; back on the original window");
    }

    /**
     * A snapshot of one open browser window.
     *
     * @param handle the Selenium window handle
     * @param url    the URL the window is showing
     * @param title  the document title
     */
    public record WindowInfo(String handle, String url, String title) {

        /** @return {@code true} when this window is showing the given page file. */
        public boolean showsPage(String pageName) {
            return url != null && url.endsWith(pageName);
        }
    }
}
