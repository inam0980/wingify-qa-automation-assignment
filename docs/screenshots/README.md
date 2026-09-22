# Test screenshots

Captured automatically by the suite during a full `mvn clean test` run against
<https://arjitnigam.github.io/myDreams/>. Every UI test writes one, on pass as well as on failure
(`screenshot.on.success=true`); these six are a representative selection committed so the run can
be reviewed without executing anything.

The complete set for a run is written to `target/screenshots/`, and each image is also embedded
directly into `target/reports/ExtentReport.html`.

| File | Captured by | Shows |
| --- | --- | --- |
| `01-home-loading-animation.png` | `loadingAnimationIsVisibleOnArrival` | The spinner and "Loading your dream portal…" on arrival, before the 3-second timeout fires |
| `02-home-loaded.png` | `mainContentAndButtonBecomeVisibleAfterLoading` | The revealed main content and the "My Dreams" button |
| `03-my-dreams-opens-two-tabs.png` | `clickingMyDreamsOpensBothPagesInNewTabs` | The state after the click that opens the diary and the summary in new tabs |
| `04-dreams-diary-table.png` | `everyRowHasAllThreeColumnsFilled` | The ten-row dream log with all three columns populated |
| `05-dreams-summary-stats.png` | `summaryShowsExpectedStatistics` | The summary page showing Good 6, Bad 4, Total 10, Recurring 2 |
| `06-summary-cross-checked-with-diary.png` | `summaryMatchesTheNumbersDerivedFromTheDiary` | The summary at the point its figures are compared with numbers derived from the diary |
