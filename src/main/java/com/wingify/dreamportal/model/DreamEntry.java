package com.wingify.dreamportal.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One row of the Dreams Diary table.
 *
 * @param name      the dream name, e.g. {@code Flying over mountains}
 * @param daysAgo   the raw cell text, e.g. {@code 3 days ago}, kept verbatim so failures report
 *                  exactly what the page showed
 * @param type      the parsed dream type
 * @param rowNumber 1-based position in the table, so a failure message points at a specific row
 */
public record DreamEntry(String name, String daysAgo, DreamType type, int rowNumber) {

    private static final Pattern DAYS_PATTERN = Pattern.compile("^(\\d+)\\s+days?\\s+ago$");

    /**
     * @return the numeric part of the "Days Ago" cell
     * @throws IllegalStateException when the cell does not read like "N day(s) ago"
     */
    public int daysAgoValue() {
        Matcher matcher = DAYS_PATTERN.matcher(daysAgo.trim());
        if (!matcher.matches()) {
            throw new IllegalStateException(
                    "Row " + rowNumber + " has a 'Days Ago' value of '" + daysAgo
                            + "', which does not match the expected 'N day(s) ago' format");
        }
        return Integer.parseInt(matcher.group(1));
    }

    /** @return {@code true} when the cell text follows the "N day(s) ago" convention. */
    public boolean hasWellFormedDaysAgo() {
        return DAYS_PATTERN.matcher(daysAgo.trim()).matches();
    }

    @Override
    public String toString() {
        return "row %d [%s | %s | %s]".formatted(rowNumber, name, daysAgo, type);
    }
}
