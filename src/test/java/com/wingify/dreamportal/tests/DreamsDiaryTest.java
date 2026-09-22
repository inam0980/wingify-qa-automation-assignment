package com.wingify.dreamportal.tests;

import com.wingify.dreamportal.config.ConfigReader;
import com.wingify.dreamportal.model.DreamEntry;
import com.wingify.dreamportal.model.DreamType;
import com.wingify.dreamportal.pages.DreamsDiaryPage;
import com.wingify.dreamportal.utils.TestLog;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Covers {@code dreams-diary.html} — the dream log table.
 */
@Epic("Dream Portal")
@Feature("Dreams Diary (dreams-diary.html)")
public class DreamsDiaryTest extends BaseTest {

    @Test(description = "The diary page loads with the expected title and column headers",
            groups = {"ui", "smoke"})
    @Story("Table structure")
    @Severity(SeverityLevel.NORMAL)
    public void diaryPageHasExpectedTitleAndHeaders() {
        DreamsDiaryPage diary = diaryPage().open();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(diary.getPageTitle(), DreamsDiaryPage.EXPECTED_TITLE,
                "The diary document title did not match");
        softly.assertEquals(diary.getHeaders(), DreamsDiaryPage.EXPECTED_HEADERS,
                "The table headers did not match the expected columns");
        softly.assertAll();

        TestLog.pass("Headers are %s", DreamsDiaryPage.EXPECTED_HEADERS);
    }

    @Test(description = "The diary contains exactly ten dream entries",
            groups = {"ui", "smoke"})
    @Story("Table structure")
    @Severity(SeverityLevel.BLOCKER)
    @Description("The assignment specifies exactly 10 entries - not at least 10.")
    public void diaryContainsExactlyTenEntries() {
        DreamsDiaryPage diary = diaryPage().open();

        int expected = ConfigReader.getInt("expected.dream.count", 10);
        int actual = diary.getEntryCount();

        TestLog.info("The diary table rendered %d entries", actual);
        assertEquals(actual, expected,
                "The diary should contain exactly %d entries but rendered %d".formatted(expected, actual));

        TestLog.pass("Found exactly %d dream entries", expected);
    }

    @Test(description = "Every dream type is either Good or Bad",
            groups = {"ui", "smoke"})
    @Story("Data validity")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Reads the raw cell text rather than the parsed model, so an unexpected value is "
            + "reported as a readable failure naming the row instead of an exception.")
    public void everyDreamTypeIsGoodOrBad() {
        DreamsDiaryPage diary = diaryPage().open();
        List<String> labels = diary.getDreamTypeLabels();

        TestLog.attachText("Dream type column", String.join(System.lineSeparator(), labels));

        SoftAssert softly = new SoftAssert();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            softly.assertNotNull(DreamType.parseOrNull(label),
                    "Row %d has dream type '%s'; only 'Good' and 'Bad' are allowed"
                            .formatted(i + 1, label));
        }
        softly.assertAll();

        Set<String> distinct = new LinkedHashSet<>(labels);
        TestLog.pass("All %d rows use only %s", labels.size(), distinct);
    }

    @Test(description = "Every row has all three columns filled",
            groups = {"ui", "smoke"})
    @Story("Data validity")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Checks both that each row has three cells and that none of them is blank.")
    public void everyRowHasAllThreeColumnsFilled() {
        DreamsDiaryPage diary = diaryPage().open();
        List<List<String>> rows = diary.getRawRows();

        SoftAssert softly = new SoftAssert();
        for (int i = 0; i < rows.size(); i++) {
            List<String> cells = rows.get(i);
            int rowNumber = i + 1;

            softly.assertEquals(cells.size(), DreamsDiaryPage.EXPECTED_HEADERS.size(),
                    "Row %d has %d cells but %d columns were expected"
                            .formatted(rowNumber, cells.size(), DreamsDiaryPage.EXPECTED_HEADERS.size()));

            for (int column = 0; column < cells.size(); column++) {
                String columnName = column < DreamsDiaryPage.EXPECTED_HEADERS.size()
                        ? DreamsDiaryPage.EXPECTED_HEADERS.get(column)
                        : "column " + (column + 1);
                softly.assertFalse(cells.get(column).isBlank(),
                        "Row %d has an empty '%s' cell".formatted(rowNumber, columnName));
            }
        }
        softly.assertAll();

        TestLog.attachText("Diary table as read by the suite", renderTable(rows));
        TestLog.pass("All %d rows have %d filled columns",
                rows.size(), DreamsDiaryPage.EXPECTED_HEADERS.size());
    }

    @Test(description = "Every 'Days Ago' value reads like 'N day(s) ago'",
            groups = {"ui"})
    @Story("Data validity")
    @Severity(SeverityLevel.NORMAL)
    @Description("Beyond the assignment's 'not empty' requirement: a filled cell holding "
            + "'yesterday' or '3' would pass that check while still being wrong for the column.")
    public void daysAgoValuesAreWellFormedAndSequential() {
        DreamsDiaryPage diary = diaryPage().open();
        List<DreamEntry> entries = diary.getAllEntries();

        SoftAssert softly = new SoftAssert();
        for (DreamEntry entry : entries) {
            softly.assertTrue(entry.hasWellFormedDaysAgo(),
                    "Row %d has a 'Days Ago' value of '%s', which is not in the 'N day(s) ago' format"
                            .formatted(entry.rowNumber(), entry.daysAgo()));
        }
        softly.assertAll();

        List<Integer> days = entries.stream().map(DreamEntry::daysAgoValue).toList();
        TestLog.info("Days-ago values in document order: %s", days);

        // The log reads newest first; an out-of-order row would mean the journal is mis-sorted.
        for (int i = 1; i < days.size(); i++) {
            assertTrue(days.get(i) > days.get(i - 1),
                    "The diary is not ordered newest first: row %d (%d days ago) is not older than row %d (%d days ago)"
                            .formatted(i + 1, days.get(i), i, days.get(i - 1)));
        }

        TestLog.pass("All %d 'Days Ago' values are well formed and ordered newest first", days.size());
    }

    @Test(description = "The Good and Bad entries split six to four",
            groups = {"ui"})
    @Story("Data validity")
    @Severity(SeverityLevel.CRITICAL)
    public void dreamTypesSplitAsExpected() {
        DreamsDiaryPage diary = diaryPage().open();
        Map<DreamType, Long> counts = diary.countByType();

        long good = counts.getOrDefault(DreamType.GOOD, 0L);
        long bad = counts.getOrDefault(DreamType.BAD, 0L);
        TestLog.info("Diary breakdown: %d Good, %d Bad", good, bad);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(good, ConfigReader.getInt("expected.good.dreams", 6),
                "The number of Good dreams in the diary did not match");
        softly.assertEquals(bad, ConfigReader.getInt("expected.bad.dreams", 4),
                "The number of Bad dreams in the diary did not match");
        softly.assertEquals(good + bad, (long) ConfigReader.getInt("expected.dream.count", 10),
                "Good plus Bad did not add up to the total number of entries");
        softly.assertAll();

        TestLog.pass("Diary splits %d Good / %d Bad", good, bad);
    }

    @Test(description = "'Flying over mountains' and 'Lost in maze' are the recurring dreams",
            groups = {"ui", "smoke"})
    @Story("Recurring dreams")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A recurring dream is a dream name that appears in more than one entry. The two "
            + "named dreams are asserted explicitly, and the set is asserted to contain nothing "
            + "else, so an unexpected duplicate is caught too.")
    public void recurringDreamsAreIdentifiedCorrectly() {
        DreamsDiaryPage diary = diaryPage().open();

        Map<String, Long> occurrences = diary.countByName();
        TestLog.attachText("Dream name occurrences", occurrences.entrySet().stream()
                .map(e -> "%-25s %d".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(System.lineSeparator())));

        Set<String> recurring = diary.getRecurringDreamNames();
        Set<String> expected = Arrays.stream(
                        ConfigReader.get("expected.recurring.names", "").split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        TestLog.info("Recurring dreams found: %s", recurring);

        SoftAssert softly = new SoftAssert();
        for (String expectedName : expected) {
            softly.assertTrue(recurring.contains(expectedName),
                    "'%s' appears %d time(s) and was not counted as recurring"
                            .formatted(expectedName, occurrences.getOrDefault(expectedName, 0L)));
        }
        softly.assertEquals(recurring, expected,
                "The set of recurring dreams did not match the expected set");
        softly.assertEquals(recurring.size(), ConfigReader.getInt("expected.recurring.dreams", 2),
                "The number of recurring dreams did not match");
        softly.assertAll();

        TestLog.pass("Recurring dreams are exactly %s", expected);
    }

    private static String renderTable(List<List<String>> rows) {
        StringBuilder rendered = new StringBuilder();
        rendered.append("%-25s %-12s %s".formatted(
                DreamsDiaryPage.EXPECTED_HEADERS.get(0),
                DreamsDiaryPage.EXPECTED_HEADERS.get(1),
                DreamsDiaryPage.EXPECTED_HEADERS.get(2)));
        for (List<String> cells : rows) {
            rendered.append(System.lineSeparator())
                    .append("%-25s %-12s %s".formatted(
                            cells.size() > 0 ? cells.get(0) : "",
                            cells.size() > 1 ? cells.get(1) : "",
                            cells.size() > 2 ? cells.get(2) : ""));
        }
        return rendered.toString();
    }
}
