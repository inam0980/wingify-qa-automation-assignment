package com.wingify.dreamportal.pages;

import com.wingify.dreamportal.model.DreamEntry;
import com.wingify.dreamportal.model.DreamType;
import io.qameta.allure.Step;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * {@code dreams-diary.html} — the dream log table.
 *
 * <p>This page is the source of truth for the suite: the summary page is verified against numbers
 * derived here rather than against hard-coded constants, so a change to the log is caught as a
 * mismatch instead of silently passing.
 */
public class DreamsDiaryPage extends BasePage {

    public static final String PAGE_NAME = "dreams-diary.html";
    public static final String EXPECTED_TITLE = "Dreams Diary";

    /** Column order as rendered in the table header. */
    public static final List<String> EXPECTED_HEADERS = List.of("Dream Name", "Days Ago", "Dream Type");

    private static final int NAME_COLUMN = 0;
    private static final int DAYS_AGO_COLUMN = 1;
    private static final int TYPE_COLUMN = 2;

    private final By table = By.id("dreamsDiary");
    private final By headerCells = By.cssSelector("#dreamsDiary thead th");
    private final By rows = By.cssSelector("#dreamsDiary tbody tr");

    public DreamsDiaryPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open the Dreams Diary page")
    public DreamsDiaryPage open() {
        navigateTo(url(PAGE_NAME));
        waitForVisible(table);
        return this;
    }

    /** Waits for the table to render — used when the page was reached via a new tab, not a navigation. */
    @Step("Wait for the dreams table to render")
    public DreamsDiaryPage waitForTable() {
        waitForVisible(table);
        return this;
    }

    @Step("Count the dream entries")
    public int getEntryCount() {
        return findAll(rows).size();
    }

    public List<String> getHeaders() {
        return findAll(headerCells).stream().map(e -> e.getText().trim()).toList();
    }

    /**
     * Reads every row as raw cell text, with no parsing or validation applied.
     *
     * <p>Used by the "every column is filled" check, which has to be able to see a blank cell
     * rather than fail while parsing it.
     *
     * @return one list of cell strings per row, in document order
     */
    @Step("Read every diary row as raw text")
    public List<List<String>> getRawRows() {
        List<List<String>> result = new ArrayList<>();
        for (WebElement row : findAll(rows)) {
            result.add(row.findElements(By.tagName("td")).stream()
                    .map(cell -> cell.getText().trim())
                    .collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * Reads every row into a validated {@link DreamEntry}.
     *
     * @return the parsed entries in document order
     * @throws IllegalArgumentException when a row carries a dream type other than Good or Bad
     */
    @Step("Read every diary row into the dream model")
    public List<DreamEntry> getAllEntries() {
        List<List<String>> rawRows = getRawRows();
        List<DreamEntry> entries = new ArrayList<>(rawRows.size());
        for (int i = 0; i < rawRows.size(); i++) {
            List<String> cells = rawRows.get(i);
            int rowNumber = i + 1;
            if (cells.size() != EXPECTED_HEADERS.size()) {
                throw new IllegalStateException(
                        "Row " + rowNumber + " has " + cells.size() + " cells but "
                                + EXPECTED_HEADERS.size() + " were expected");
            }
            entries.add(new DreamEntry(
                    cells.get(NAME_COLUMN),
                    cells.get(DAYS_AGO_COLUMN),
                    DreamType.fromLabel(cells.get(TYPE_COLUMN)),
                    rowNumber));
        }
        log.info("Read {} dream entries from the diary", entries.size());
        return entries;
    }

    /** @return the raw dream-type cell text of every row, unparsed. */
    public List<String> getDreamTypeLabels() {
        return getRawRows().stream()
                .map(cells -> cells.size() > TYPE_COLUMN ? cells.get(TYPE_COLUMN) : "")
                .toList();
    }

    /** @return the dream name of every row, duplicates included, in document order. */
    public List<String> getDreamNames() {
        return getRawRows().stream()
                .map(cells -> cells.isEmpty() ? "" : cells.get(NAME_COLUMN))
                .toList();
    }

    /** @return how many rows carry each dream type. */
    @Step("Count the entries of each dream type")
    public Map<DreamType, Long> countByType() {
        return getAllEntries().stream()
                .collect(Collectors.groupingBy(DreamEntry::type, Collectors.counting()));
    }

    /** @return how many times each dream name appears, keyed in first-seen order. */
    @Step("Count how often each dream name appears")
    public Map<String, Long> countByName() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String name : getDreamNames()) {
            counts.merge(name, 1L, Long::sum);
        }
        return counts;
    }

    /**
     * Applies the portal's own definition of a recurring dream: a dream name that shows up in more
     * than one entry. The summary page's "Recurring Dreams" figure counts distinct such names, not
     * the total number of repeated rows.
     *
     * @return the distinct names that appear at least twice, in first-seen order
     */
    @Step("Work out which dreams are recurring")
    public Set<String> getRecurringDreamNames() {
        Set<String> recurring = countByName().entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        log.info("Recurring dreams found: {}", recurring);
        return recurring;
    }
}
