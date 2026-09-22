package com.wingify.dreamportal.model;

import java.util.Locale;

/**
 * The only two dream classifications the portal is allowed to show.
 *
 * <p>Modelled as an enum rather than a raw string so that "the dream types are only Good or Bad"
 * is enforced while parsing, instead of by scattered string comparisons in the tests.
 */
public enum DreamType {

    GOOD,
    BAD;

    /**
     * @param label the cell text as rendered in the diary table
     * @return the matching type
     * @throws IllegalArgumentException when the portal shows anything other than Good or Bad
     */
    public static DreamType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Dream type cell was empty");
        }
        return switch (label.trim().toUpperCase(Locale.ROOT)) {
            case "GOOD" -> GOOD;
            case "BAD" -> BAD;
            default -> throw new IllegalArgumentException(
                    "Unexpected dream type '" + label + "' - only 'Good' and 'Bad' are valid");
        };
    }

    /** @return {@code null} instead of throwing, for callers that assert on the failure themselves. */
    public static DreamType parseOrNull(String label) {
        try {
            return fromLabel(label);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** @return the label exactly as the portal renders it, e.g. {@code Good}. */
    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
