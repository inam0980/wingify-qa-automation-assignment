package com.wingify.dreamportal.pages;

import com.wingify.dreamportal.config.ConfigReader;
import java.time.Duration;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared plumbing for every page object: the driver handle, one explicit wait, and the small set
 * of interactions the pages actually need.
 *
 * <p>Page objects expose intent ("is the loading animation gone?"), never raw locators, so a
 * markup change is fixed in one file and the tests stay untouched.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Logger log = LogManager.getLogger(getClass());

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(
                driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds", 15)));
    }

    /** @return the absolute URL of this page, built from the configured {@code base.url}. */
    protected static String url(String pageName) {
        String baseUrl = ConfigReader.getRequired("base.url");
        return baseUrl.endsWith("/") ? baseUrl + pageName : baseUrl + "/" + pageName;
    }

    protected void navigateTo(String pageUrl) {
        log.info("Navigating to {}", pageUrl);
        driver.get(pageUrl);
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * @return whether the element is present in the DOM <em>and</em> rendered. Returns
     *     {@code false} rather than throwing when the element is absent, so it is safe to use
     *     directly inside an assertion.
     */
    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    protected List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    /** @return {@code true} when the element carries the given CSS class. */
    protected boolean hasClass(WebElement element, String cssClass) {
        String classAttribute = element.getDomAttribute("class");
        if (classAttribute == null || classAttribute.isBlank()) {
            return false;
        }
        for (String actual : classAttribute.trim().split("\\s+")) {
            if (actual.equals(cssClass)) {
                return true;
            }
        }
        return false;
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
