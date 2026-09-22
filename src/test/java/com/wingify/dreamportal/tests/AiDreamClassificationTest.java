package com.wingify.dreamportal.tests;

import com.wingify.dreamportal.ai.DreamClassifier;
import com.wingify.dreamportal.ai.DreamClassifierFactory;
import com.wingify.dreamportal.config.ConfigReader;
import com.wingify.dreamportal.driver.DriverFactory;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Part 2 — AI-based validation.
 *
 * <p>Each dream name is sent to a language model, which is asked to classify it as Good or Bad
 * from the name alone. The verdict is then compared with the label the portal shows.
 *
 * <p>Two design notes worth stating, because they are what make this test useful rather than
 * decorative:
 *
 * <ul>
 *   <li><b>The browser is used once.</b> The diary is scraped a single time in {@code @BeforeClass}
 *       and the browser is closed; the ten classifications then run without a browser at all.
 *       Launching Chrome per dream name would add nothing but minutes.</li>
 *   <li><b>Disagreement is reported two ways.</b> Each dream is asserted individually, so a
 *       mismatch names the dream and both verdicts. A summary test then reports the overall
 *       agreement rate, which is the number that actually says whether the labelling and the
 *       model agree — a single subjective dream should not be read as the model failing.</li>
 * </ul>
 */
@Epic("Dream Portal")
@Feature("AI-based dream classification")
public class AiDreamClassificationTest {

    private static final Logger LOG = LogManager.getLogger(AiDreamClassificationTest.class);

    /** Scraped once from the live diary, then reused by every method in this class. */
    private static final List<DreamEntry> DIARY_ENTRIES = Collections.synchronizedList(new ArrayList<>());

    /** One row per dream, collected as the data-driven test runs, for the summary at the end. */
    private static final List<Comparison> COMPARISONS = Collections.synchronizedList(new ArrayList<>());

    private static DreamClassifier classifier;

    @BeforeClass(alwaysRun = true)
    public void readDiaryAndPrepareClassifier() {
        loadDiaryEntries();
        classifier = DreamClassifierFactory.create();

        LOG.info("AI classifier in use: {}", classifier.providerName());
        if (!classifier.isModelBacked()) {
            LOG.warn("No AI provider is configured, so the local rule-based classifier is being "
                    + "used. Set AI_PROVIDER and the matching API key in .env to run these "
                    + "assertions against a real model.");
        }
    }

    /**
     * Opens the diary once and caches its rows.
     *
     * <p>Idempotent and synchronized so it is safe to call from both {@code @BeforeClass} and the
     * data provider, whichever TestNG happens to reach first.
     */
    private static synchronized void loadDiaryEntries() {
        if (!DIARY_ENTRIES.isEmpty()) {
            return;
        }
        WebDriver driver = DriverFactory.createDriver();
        try {
            DIARY_ENTRIES.addAll(new DreamsDiaryPage(driver).open().getAllEntries());
            LOG.info("Cached {} dream entries for AI classification", DIARY_ENTRIES.size());
        } finally {
            DriverFactory.quitDriver();
        }
    }

    /** @return one row per diary entry: the dream name, its label on the portal, and the row number. */
    @DataProvider(name = "dreamNames")
    public Object[][] dreamNames() {
        loadDiaryEntries();
        return DIARY_ENTRIES.stream()
                .map(entry -> new Object[] {entry.name(), entry.type(), entry.rowNumber()})
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "dreamNames",
            description = "The AI verdict for each dream name matches the portal's label",
            groups = {"ai"})
    @Story("Classification agreement")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sends the dream name to the configured model with an instruction to answer with "
            + "a single word, then asserts that answer against the Dream Type column.")
    public void aiClassificationMatchesTheDiary(String dreamName, DreamType expected, int rowNumber) {
        if (classifier == null) {
            throw new SkipException("No classifier was created; check the @BeforeClass logs");
        }

        DreamClassifier.DreamClassification result = classifier.classify(dreamName);
        boolean agrees = result.type() == expected;
        COMPARISONS.add(new Comparison(dreamName, expected, result.type(), result.rawResponse()));

        TestLog.info("Row %d: '%s' - portal says %s, %s says %s",
                rowNumber, dreamName, expected.label(), classifier.providerName(), result.type().label());
        TestLog.attachText("Raw model response for '" + dreamName + "'", result.rawResponse());

        assertEquals(result.type(), expected,
                "%s classified '%s' as %s but the portal labels it %s"
                        .formatted(classifier.providerName(), dreamName,
                                result.type().label(), expected.label()));

        TestLog.pass("'%s' -> %s, agreeing with the portal", dreamName, result.type().label());
        assertTrue(agrees, "Verdicts should agree");
    }

    @Test(dependsOnMethods = "aiClassificationMatchesTheDiary",
            alwaysRun = true,
            description = "The AI agrees with the portal on enough of the dreams",
            groups = {"ai"})
    @Story("Classification agreement")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Reports the overall agreement rate across all ten dreams and asserts it against "
            + "ai.minimum.agreement.ratio. Classifying a dream by tone is a judgement call, so "
            + "the aggregate rate is the meaningful signal rather than any single verdict.")
    public void aiAgreementRateMeetsTheThreshold() {
        if (COMPARISONS.isEmpty()) {
            throw new SkipException("No classifications were recorded, so there is nothing to summarise");
        }

        long agreements = COMPARISONS.stream().filter(Comparison::agrees).count();
        int total = COMPARISONS.size();
        double ratio = (double) agreements / total;
        double threshold = Double.parseDouble(
                ConfigReader.get("ai.minimum.agreement.ratio", "0.7"));

        TestLog.attachText("AI vs portal, dream by dream", renderComparisonTable());
        TestLog.info("%s agreed with the portal on %d of %d dreams (%.0f%%); the threshold is %.0f%%",
                classifier.providerName(), agreements, total, ratio * 100, threshold * 100);

        List<String> disagreements = COMPARISONS.stream()
                .filter(comparison -> !comparison.agrees())
                .map(comparison -> "'%s' (portal: %s, AI: %s)".formatted(
                        comparison.dreamName(), comparison.expected().label(),
                        comparison.actual().label()))
                .toList();
        if (!disagreements.isEmpty()) {
            TestLog.warn("The model disagreed on: %s", String.join("; ", disagreements));
        }

        assertTrue(ratio >= threshold,
                "%s agreed on only %d of %d dreams (%.0f%%), below the required %.0f%%. Disagreements: %s"
                        .formatted(classifier.providerName(), agreements, total, ratio * 100,
                                threshold * 100, disagreements));

        TestLog.pass("Agreement rate is %.0f%% across %d dreams", ratio * 100, total);
    }

    @Test(description = "The classifier refuses an empty dream name instead of guessing",
            groups = {"ai"})
    @Story("Classifier robustness")
    @Severity(SeverityLevel.MINOR)
    @Description("A negative case: a blank input is a caller error and must be rejected, not "
            + "silently classified as Good.")
    public void classifierRejectsAnEmptyDreamName() {
        DreamClassifier active = classifier == null ? DreamClassifierFactory.create() : classifier;

        try {
            DreamClassifier.DreamClassification result = active.classify("   ");
            org.testng.Assert.fail(
                    "Expected a blank dream name to be rejected but it was classified as "
                            + result.type());
        } catch (DreamClassifier.ClassificationException expectedFailure) {
            TestLog.pass("A blank dream name was rejected: %s", expectedFailure.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeClassifier() {
        if (classifier != null) {
            try {
                classifier.close();
            } catch (Exception e) {
                LOG.warn("Ignoring error while closing the classifier: {}", e.getMessage());
            }
        }
        DriverFactory.quitDriver();
    }

    private String renderComparisonTable() {
        String header = "%-25s %-8s %-8s %s".formatted("DREAM", "PORTAL", "AI", "AGREES");
        String rows = COMPARISONS.stream()
                .map(comparison -> "%-25s %-8s %-8s %s".formatted(
                        comparison.dreamName(),
                        comparison.expected().label(),
                        comparison.actual().label(),
                        comparison.agrees() ? "yes" : "NO"))
                .collect(Collectors.joining(System.lineSeparator()));
        return header + System.lineSeparator() + rows;
    }

    /**
     * One dream's portal label set against the model's verdict.
     *
     * @param dreamName   the dream that was classified
     * @param expected    the label shown on the portal
     * @param actual      the model's verdict
     * @param rawResponse the model's answer before parsing
     */
    private record Comparison(String dreamName, DreamType expected, DreamType actual, String rawResponse) {

        boolean agrees() {
            return expected == actual;
        }
    }
}
