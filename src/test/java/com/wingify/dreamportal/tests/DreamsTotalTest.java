package com.wingify.dreamportal.tests;

import com.wingify.dreamportal.config.ConfigReader;
import com.wingify.dreamportal.model.DreamEntry;
import com.wingify.dreamportal.model.DreamType;
import com.wingify.dreamportal.pages.DreamsDiaryPage;
import com.wingify.dreamportal.pages.DreamsTotalPage;
import com.wingify.dreamportal.utils.TestLog;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;

/**
 * Covers {@code dreams-total.html} — the summary page.
 *
 * <p>Two complementary angles: the figures are asserted against the values the assignment
 * specifies, and then asserted again against numbers derived live from the diary. The first
 * catches a regression in the summary; the second catches the two pages drifting apart, which a
 * fixed-value check alone would miss.
 */
@Epic("Dream Portal")
@Feature("Dreams Summary (dreams-total.html)")
public class DreamsTotalTest extends BaseTest {

    @Test(description = "The summary page loads with the expected title",
            groups = {"ui", "smoke"})
    @Story("Page loads")
    @Severity(SeverityLevel.MINOR)
    public void summaryPageHasExpectedTitle() {
        DreamsTotalPage summary = totalPage().open();

        assertEquals(summary.getPageTitle(), DreamsTotalPage.EXPECTED_TITLE,
                "The summary document title did not match");
        TestLog.pass("Title is '%s'", DreamsTotalPage.EXPECTED_TITLE);
    }

    @Test(description = "The summary shows the expected Good, Bad, Total and Recurring counts",
            groups = {"ui", "smoke"})
    @Story("Summary statistics")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Good 6, Bad 4, Total 10, Recurring 2, as specified in the assignment.")
    public void summaryShowsExpectedStatistics() {
        DreamsTotalPage summary = totalPage().open();
        Map<String, Integer> stats = summary.getAllStats();

        TestLog.attachText("Summary statistics", stats.entrySet().stream()
                .map(e -> "%-20s %d".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(System.lineSeparator())));

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(summary.getGoodDreams(), ConfigReader.getInt("expected.good.dreams", 6),
                "'Good Dreams' did not match");
        softly.assertEquals(summary.getBadDreams(), ConfigReader.getInt("expected.bad.dreams", 4),
                "'Bad Dreams' did not match");
        softly.assertEquals(summary.getTotalDreams(), ConfigReader.getInt("expected.total.dreams", 10),
                "'Total Dreams' did not match");
        softly.assertEquals(summary.getRecurringDreams(),
                ConfigReader.getInt("expected.recurring.dreams", 2),
                "'Recurring Dreams' did not match");
        softly.assertEquals(summary.getDreamsThisWeek(),
                ConfigReader.getInt("expected.dreams.this.week", 7),
                "'Dreams This Week' did not match");
        softly.assertAll();

        TestLog.pass("All five summary statistics match the expected values");
    }

    @Test(description = "Good plus Bad equals the Total shown on the summary page",
            groups = {"ui"})
    @Story("Summary statistics")
    @Severity(SeverityLevel.CRITICAL)
    @Description("An internal consistency check that holds regardless of the specific numbers.")
    public void summaryTotalsAreInternallyConsistent() {
        DreamsTotalPage summary = totalPage().open();

        int good = summary.getGoodDreams();
        int bad = summary.getBadDreams();
        int total = summary.getTotalDreams();

        TestLog.info("Summary reports %d Good + %d Bad against a total of %d", good, bad, total);
        assertEquals(good + bad, total,
                "Good (%d) plus Bad (%d) does not equal the reported total (%d)"
                        .formatted(good, bad, total));

        TestLog.pass("%d + %d = %d", good, bad, total);
    }

    @Test(description = "Every summary figure matches the numbers derived from the diary",
            groups = {"ui", "smoke"})
    @Story("Cross-page consistency")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Reads the diary, derives the statistics from its rows, then compares them with "
            + "what the summary page claims. This is the check that would catch the two pages "
            + "disagreeing after a data change.")
    public void summaryMatchesTheNumbersDerivedFromTheDiary() {
        DreamsDiaryPage diary = diaryPage().open();
        List<DreamEntry> entries = diary.getAllEntries();
        Map<DreamType, Long> countsByType = diary.countByType();
        Set<String> recurring = diary.getRecurringDreamNames();

        long derivedGood = countsByType.getOrDefault(DreamType.GOOD, 0L);
        long derivedBad = countsByType.getOrDefault(DreamType.BAD, 0L);
        long derivedTotal = entries.size();
        long derivedRecurring = recurring.size();
        long derivedThisWeek = entries.stream().filter(e -> e.daysAgoValue() <= 7).count();

        TestLog.attachText("Derived from the diary", """
                Good dreams      %d
                Bad dreams       %d
                Total dreams     %d
                Dreams this week %d
                Recurring dreams %d %s"""
                .formatted(derivedGood, derivedBad, derivedTotal, derivedThisWeek,
                        derivedRecurring, recurring));

        DreamsTotalPage summary = totalPage().open();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals((long) summary.getGoodDreams(), derivedGood,
                "The summary's Good count disagrees with the diary");
        softly.assertEquals((long) summary.getBadDreams(), derivedBad,
                "The summary's Bad count disagrees with the diary");
        softly.assertEquals((long) summary.getTotalDreams(), derivedTotal,
                "The summary's Total count disagrees with the number of diary rows");
        softly.assertEquals((long) summary.getRecurringDreams(), derivedRecurring,
                "The summary's Recurring count disagrees with the diary: recurring names are " + recurring);
        softly.assertEquals((long) summary.getDreamsThisWeek(), derivedThisWeek,
                "The summary's 'Dreams This Week' disagrees with the diary rows from the last 7 days");
        softly.assertAll();

        TestLog.pass("The summary agrees with the diary on all five statistics");
    }

    @Test(description = "The recurring count reflects the dreams named in the assignment",
            groups = {"ui"})
    @Story("Cross-page consistency")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Asserts that 'Flying over mountains' and 'Lost in maze' are what the summary's "
            + "Recurring Dreams figure is counting, using the diary as the source of truth.")
    public void recurringCountIsBackedByTheNamedDreams() {
        DreamsDiaryPage diary = diaryPage().open();
        Map<String, Long> occurrences = diary.countByName();
        Set<String> recurring = diary.getRecurringDreamNames();

        SoftAssert softly = new SoftAssert();
        for (String name : ConfigReader.get("expected.recurring.names", "").split(",")) {
            String expectedName = name.trim();
            if (expectedName.isEmpty()) {
                continue;
            }
            long seen = occurrences.getOrDefault(expectedName, 0L);
            TestLog.info("'%s' appears %d time(s) in the diary", expectedName, seen);
            softly.assertTrue(seen > 1,
                    "'%s' appears %d time(s), so it cannot be counted as recurring"
                            .formatted(expectedName, seen));
            softly.assertTrue(recurring.contains(expectedName),
                    "'%s' was not included in the recurring set %s".formatted(expectedName, recurring));
        }
        softly.assertAll();

        DreamsTotalPage summary = totalPage().open();
        assertEquals(summary.getRecurringDreams(), recurring.size(),
                "The summary reports %d recurring dreams but the diary contains %d: %s"
                        .formatted(summary.getRecurringDreams(), recurring.size(), recurring));

        TestLog.pass("The summary's recurring count of %d is backed by %s",
                recurring.size(), recurring);
    }
}
