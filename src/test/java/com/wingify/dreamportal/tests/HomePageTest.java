package com.wingify.dreamportal.tests;

import com.wingify.dreamportal.config.ConfigReader;
import com.wingify.dreamportal.pages.DreamsDiaryPage;
import com.wingify.dreamportal.pages.DreamsTotalPage;
import com.wingify.dreamportal.pages.HomePage;
import com.wingify.dreamportal.utils.TestLog;
import com.wingify.dreamportal.utils.WindowManager;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Covers {@code index.html} — the loading sequence and the two tabs the "My Dreams" button opens.
 */
@Epic("Dream Portal")
@Feature("Home page (index.html)")
public class HomePageTest extends BaseTest {

    @Test(description = "The home page loads with the expected document title",
            groups = {"ui", "smoke"})
    @Story("Page loads")
    @Severity(SeverityLevel.MINOR)
    @Description("A cheap first check that the suite is pointed at the right application.")
    public void homePageHasExpectedTitle() {
        HomePage home = homePage().open();

        TestLog.info("Document title is '%s'", home.getPageTitle());
        assertEquals(home.getPageTitle(), HomePage.EXPECTED_TITLE,
                "The home page document title did not match");
        TestLog.pass("Title is '%s'", HomePage.EXPECTED_TITLE);
    }

    @Test(description = "The loading animation is on screen as soon as the page opens",
            groups = {"ui", "smoke"})
    @Story("Loading animation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On arrival the spinner must be visible and the main content must still be hidden.")
    public void loadingAnimationIsVisibleOnArrival() {
        HomePage home = homePage().open();

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(home.isLoadingAnimationDisplayed(),
                "The loading animation was not visible when the page opened");
        softly.assertTrue(home.isSpinnerDisplayed(),
                "The spinner inside the loading animation was not visible");
        softly.assertFalse(home.isMainContentDisplayed(),
                "The main content was already visible while the page was still loading");
        softly.assertFalse(home.isDreamButtonDisplayed(),
                "The 'My Dreams' button was already visible while the page was still loading");
        softly.assertAll();

        TestLog.pass("Spinner shown and main content still hidden on arrival");
    }

    @Test(description = "The loading animation disappears after roughly three seconds",
            groups = {"ui", "smoke"})
    @Story("Loading animation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("script.js hides the spinner via setTimeout(..., 3000). The measured window is "
            + "asserted against a configurable tolerance so normal page-load jitter does not "
            + "make the check flaky, while a spinner that never clears still fails.")
    public void loadingAnimationDisappearsAfterAboutThreeSeconds() {
        HomePage home = homePage().open();

        assertTrue(home.isLoadingAnimationDisplayed(),
                "The loading animation was not visible, so its disappearance cannot be timed");

        Duration visibleFor = home.waitForLoadingAnimationToDisappear();
        double seconds = visibleFor.toMillis() / 1000.0;

        double minSeconds = Double.parseDouble(ConfigReader.get("loading.animation.min.seconds", "1.5"));
        double maxSeconds = Double.parseDouble(ConfigReader.get("loading.animation.max.seconds", "6"));
        String expected = ConfigReader.get("loading.animation.expected.seconds", "3");

        TestLog.info("Loading animation stayed visible for %.2f s (expected about %s s, allowed %.1f-%.1f s)",
                seconds, expected, minSeconds, maxSeconds);

        assertFalse(home.isLoadingAnimationDisplayed(),
                "The loading animation was still visible after the wait");
        assertTrue(seconds >= minSeconds && seconds <= maxSeconds,
                "The loading animation was visible for %.2f s, outside the allowed %.1f-%.1f s window"
                        .formatted(seconds, minSeconds, maxSeconds));

        TestLog.pass("Loading animation cleared after %.2f s", seconds);
    }

    @Test(description = "The main content and the 'My Dreams' button appear once loading finishes",
            groups = {"ui", "smoke"})
    @Story("Loading animation")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Checks both the visibility of the revealed elements and the CSS classes that "
            + "drive their reveal animation, so a half-applied transition is still caught.")
    public void mainContentAndButtonBecomeVisibleAfterLoading() {
        HomePage home = homePage().open();
        home.waitForPortalToLoad();

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(home.isLoadingAnimationDisplayed(),
                "The loading animation was still visible after loading finished");
        softly.assertTrue(home.isMainContentDisplayed(), "The main content did not become visible");
        softly.assertTrue(home.isDreamButtonDisplayed(),
                "The 'My Dreams' button did not become visible");
        softly.assertTrue(home.isMainContentFadedIn(),
                "The main content is displayed but its 'show' reveal class was never applied");
        softly.assertTrue(home.isDreamButtonRevealed(),
                "The 'My Dreams' button is displayed but its 'show' reveal class was never applied");
        // The stylesheet uppercases this button, so the rendered text is "MY DREAMS" while the
        // markup reads "My Dreams". Both are asserted: the rendered label case-insensitively,
        // because the casing is presentation, and the DOM text exactly, because that is content.
        String renderedLabel = home.getDreamButtonRenderedText();
        softly.assertTrue(renderedLabel.equalsIgnoreCase("My Dreams"),
                "The rendered button label was '%s', not 'My Dreams'".formatted(renderedLabel));
        softly.assertEquals(home.getDreamButtonSourceText(), "My Dreams",
                "The button label in the DOM was not 'My Dreams'");
        softly.assertAll();

        List<String> stats = home.getHeadlineStats();
        TestLog.info("Headline stats on the home page: %s", stats);
        TestLog.pass("Main content and 'My Dreams' button are both revealed");
    }

    @Test(description = "Clicking 'My Dreams' opens the diary and the summary in two new tabs",
            groups = {"ui", "smoke"})
    @Story("Navigation")
    @Severity(SeverityLevel.BLOCKER)
    @Description("The button calls window.open() twice. Both new tabs are identified by URL "
            + "rather than by opening order, because handle order is not guaranteed.")
    public void clickingMyDreamsOpensBothPagesInNewTabs() {
        HomePage home = homePage().open();
        home.waitForPortalToLoad();

        String originalTab = driver.getWindowHandle();
        Set<String> tabsBefore = driver.getWindowHandles();
        TestLog.info("Tabs open before the click: %d", tabsBefore.size());

        home.clickMyDreams();

        int expectedTabs = ConfigReader.getInt("expected.tabs.after.click", 3);
        Set<String> tabsAfter = WindowManager.waitForWindowCount(driver, expectedTabs);
        Set<String> openedTabs = WindowManager.newHandles(tabsBefore, tabsAfter);

        assertEquals(openedTabs.size(), 2,
                "Clicking 'My Dreams' should open exactly 2 new tabs");

        List<WindowManager.WindowInfo> opened =
                WindowManager.describe(driver, openedTabs, originalTab);
        TestLog.attachText("Tabs opened by the click", opened.stream()
                .map(info -> "%s -> %s".formatted(info.title(), info.url()))
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("none"));

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(
                opened.stream().anyMatch(info -> info.showsPage(DreamsDiaryPage.PAGE_NAME)),
                "No new tab opened " + DreamsDiaryPage.PAGE_NAME);
        softly.assertTrue(
                opened.stream().anyMatch(info -> info.showsPage(DreamsTotalPage.PAGE_NAME)),
                "No new tab opened " + DreamsTotalPage.PAGE_NAME);
        softly.assertEquals(driver.getWindowHandle(), originalTab,
                "The driver should be back on the original tab");
        softly.assertAll();

        TestLog.pass("Both %s and %s opened in new tabs",
                DreamsDiaryPage.PAGE_NAME, DreamsTotalPage.PAGE_NAME);
    }

    @Test(description = "The tabs opened by the button render their tables",
            groups = {"ui"},
            dependsOnMethods = "clickingMyDreamsOpensBothPagesInNewTabs")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Opening a URL is not the same as rendering it: this follows the click through "
            + "and confirms each new tab actually shows its table.")
    public void tabsOpenedByTheButtonRenderTheirContent() {
        HomePage home = homePage().open();
        home.waitForPortalToLoad();

        String originalTab = driver.getWindowHandle();
        home.clickMyDreams();
        WindowManager.waitForWindowCount(driver, ConfigReader.getInt("expected.tabs.after.click", 3));

        WindowManager.switchToPage(driver, DreamsDiaryPage.PAGE_NAME);
        int entryCount = diaryPage().waitForTable().getEntryCount();
        TestLog.info("The diary tab rendered %d entries", entryCount);
        assertTrue(entryCount > 0, "The diary tab opened but its table rendered no rows");

        WindowManager.switchToPage(driver, DreamsTotalPage.PAGE_NAME);
        var stats = totalPage().waitForTable().getAllStats();
        TestLog.info("The summary tab rendered %d statistics", stats.size());
        assertTrue(stats.size() > 0, "The summary tab opened but its table rendered no rows");

        WindowManager.closeAllExcept(driver, originalTab);
        TestLog.pass("Both opened tabs rendered their tables");
    }
}
