package com.wingify.dreamportal.pages;

import io.qameta.allure.Step;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * {@code dreams-total.html} — the summary page.
 *
 * <p>Stats are read into a label-to-count map rather than by row index, so the assertions keep
 * working if the summary rows are ever reordered, and a failure names the missing label.
 */
public class DreamsTotalPage extends BasePage {

    public static final String PAGE_NAME = "dreams-total.html";
    public static final String EXPECTED_TITLE = "Dreams Total";

    public static final String GOOD_DREAMS = "Good Dreams";
    public static final String BAD_DREAMS = "Bad Dreams";
    public static final String TOTAL_DREAMS = "Total Dreams";
    public static final String DREAMS_THIS_WEEK = "Dreams This Week";
    public static final String RECURRING_DREAMS = "Recurring Dreams";

    private final By table = By.id("dreamsTotal");
    private final By rows = By.cssSelector("#dreamsTotal tbody tr");

    public DreamsTotalPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open the Dreams Summary page")
    public DreamsTotalPage open() {
        navigateTo(url(PAGE_NAME));
        waitForVisible(table);
        return this;
    }

    /** Waits for the table to render — used when the page was reached via a new tab. */
    @Step("Wait for the summary table to render")
    public DreamsTotalPage waitForTable() {
        waitForVisible(table);
        return this;
    }

    /**
     * @return every summary row as label to count, in document order
     * @throws IllegalStateException when a count cell does not hold a whole number
     */
    @Step("Read the summary statistics")
    public Map<String, Integer> getAllStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (WebElement row : findAll(rows)) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() < 2) {
                throw new IllegalStateException(
                        "A summary row has " + cells.size() + " cells; expected a label and a count");
            }
            String label = cells.get(0).getText().trim();
            String rawCount = cells.get(1).getText().trim();
            try {
                stats.put(label, Integer.parseInt(rawCount));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Summary row '" + label + "' has a non-numeric count '" + rawCount + "'", e);
            }
        }
        log.info("Summary statistics read from the page: {}", stats);
        return stats;
    }

    /**
     * @param label one of the {@code *_DREAMS} constants on this class
     * @return the count shown against that label
     * @throws IllegalStateException when the page has no such row
     */
    @Step("Read the '{label}' statistic")
    public int getStat(String label) {
        Map<String, Integer> stats = getAllStats();
        Integer value = stats.get(label);
        if (value == null) {
            throw new IllegalStateException(
                    "The summary page has no '" + label + "' row. Rows present: " + stats.keySet());
        }
        return value;
    }

    public int getGoodDreams() {
        return getStat(GOOD_DREAMS);
    }

    public int getBadDreams() {
        return getStat(BAD_DREAMS);
    }

    public int getTotalDreams() {
        return getStat(TOTAL_DREAMS);
    }

    public int getDreamsThisWeek() {
        return getStat(DREAMS_THIS_WEEK);
    }

    public int getRecurringDreams() {
        return getStat(RECURRING_DREAMS);
    }
}
