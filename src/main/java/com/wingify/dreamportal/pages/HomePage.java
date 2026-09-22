package com.wingify.dreamportal.pages;

import io.qameta.allure.Step;
import java.time.Duration;
import java.time.Instant;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * {@code index.html} — the Dream Portal landing page.
 *
 * <p>The page shows a spinner for three seconds (a {@code setTimeout} in {@code script.js}), then
 * swaps the {@code hidden} class from the loading block onto the main content and the
 * "My Dreams" button. Because {@code .hidden} resolves to {@code display: none !important},
 * Selenium's own visibility check is an accurate signal here and no polling on CSS is needed.
 */
public class HomePage extends BasePage {

    public static final String PAGE_NAME = "index.html";
    public static final String EXPECTED_TITLE = "Dream Portal";

    private final By loadingAnimation = By.id("loadingAnimation");
    private final By spinner = By.cssSelector("#loadingAnimation .spinner");
    private final By mainContent = By.id("mainContent");
    private final By dreamButton = By.id("dreamButton");
    private final By statNumbers = By.cssSelector("#mainContent .stat-item .stat-number");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    @Step("Open the Dream Portal home page")
    public HomePage open() {
        navigateTo(url(PAGE_NAME));
        return this;
    }

    @Step("Check that the loading animation is on screen")
    public boolean isLoadingAnimationDisplayed() {
        return isDisplayed(loadingAnimation);
    }

    @Step("Check that the spinner is on screen")
    public boolean isSpinnerDisplayed() {
        return isDisplayed(spinner);
    }

    @Step("Check that the main content is on screen")
    public boolean isMainContentDisplayed() {
        return isDisplayed(mainContent);
    }

    @Step("Check that the 'My Dreams' button is on screen")
    public boolean isDreamButtonDisplayed() {
        return isDisplayed(dreamButton);
    }

    /**
     * Blocks until the spinner is gone and reports how long that took.
     *
     * <p>The clock starts when this method is called rather than at navigation, so the caller
     * decides what is being measured. Call it immediately after {@link #open()} to time the
     * advertised three-second delay.
     *
     * @return how long the loading animation stayed visible
     */
    @Step("Wait for the loading animation to disappear and measure how long it took")
    public Duration waitForLoadingAnimationToDisappear() {
        Instant start = Instant.now();
        waitForInvisible(loadingAnimation);
        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Loading animation disappeared after {} ms", elapsed.toMillis());
        return elapsed;
    }

    @Step("Wait for the main content to become visible")
    public HomePage waitForMainContent() {
        waitForVisible(mainContent);
        return this;
    }

    @Step("Wait for the 'My Dreams' button to become visible")
    public HomePage waitForDreamButton() {
        waitForClickable(dreamButton);
        return this;
    }

    /**
     * Waits out the full loading sequence: spinner gone, main content shown, button clickable.
     *
     * @return how long the loading animation was visible
     */
    @Step("Wait for the portal to finish loading")
    public Duration waitForPortalToLoad() {
        Duration loadingDuration = waitForLoadingAnimationToDisappear();
        waitForMainContent();
        waitForDreamButton();
        return loadingDuration;
    }

    /**
     * @return {@code true} once the fade-in class has landed on the main content. The element is
     *     already {@code display: block} before this, so this is the stricter "fully revealed"
     *     signal.
     */
    public boolean isMainContentFadedIn() {
        WebElement content = driver.findElement(mainContent);
        return hasClass(content, "show");
    }

    /** @return {@code true} once the button's reveal animation class has been applied. */
    public boolean isDreamButtonRevealed() {
        WebElement button = driver.findElement(dreamButton);
        return hasClass(button, "show") && !hasClass(button, "hidden");
    }

    /**
     * @return the button label as the user sees it. The stylesheet applies
     *     {@code text-transform: uppercase} to this button, so the rendered text is
     *     {@code MY DREAMS} even though the markup reads {@code My Dreams}.
     */
    public String getDreamButtonRenderedText() {
        return getText(dreamButton);
    }

    /**
     * @return the button label as written in the DOM, unaffected by CSS text transforms — the
     *     stricter of the two checks, since it pins the actual markup rather than its styling
     */
    public String getDreamButtonSourceText() {
        String textContent = driver.findElement(dreamButton).getDomProperty("textContent");
        return textContent == null ? "" : textContent.trim();
    }

    /** @return the two headline numbers rendered above the button, in document order. */
    public java.util.List<String> getHeadlineStats() {
        return findAll(statNumbers).stream().map(e -> e.getText().trim()).toList();
    }

    @Step("Click the 'My Dreams' button")
    public void clickMyDreams() {
        log.info("Clicking the 'My Dreams' button");
        waitForClickable(dreamButton).click();
    }
}
