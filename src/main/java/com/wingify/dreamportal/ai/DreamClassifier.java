package com.wingify.dreamportal.ai;

import com.wingify.dreamportal.model.DreamType;

/**
 * Classifies a dream name as {@link DreamType#GOOD} or {@link DreamType#BAD}.
 *
 * <p>Implemented once per AI provider plus a keyword-based local implementation, so the bonus
 * assertions run on any machine — with a real model when an API key is configured, and
 * deterministically when one is not.
 */
public interface DreamClassifier extends AutoCloseable {

    /** @return a human-readable provider name, used in logs and report attachments. */
    String providerName();

    /**
     * @param dreamName the dream name exactly as it appears in the diary table
     * @return the classification, including the raw model output for reporting
     * @throws ClassificationException when the provider could not be reached or its answer could
     *     not be understood
     */
    DreamClassification classify(String dreamName);

    /** @return {@code true} when this implementation calls a real model rather than local rules. */
    default boolean isModelBacked() {
        return true;
    }

    @Override
    default void close() {
    }

    /**
     * One classification result.
     *
     * @param dreamName   the input that was classified
     * @param type        the parsed verdict
     * @param rawResponse the provider's answer before parsing, kept for the report
     * @param provider    which implementation produced it
     */
    record DreamClassification(String dreamName, DreamType type, String rawResponse, String provider) {
    }

    /** Raised when a provider is unreachable or returns something that is not Good or Bad. */
    class ClassificationException extends RuntimeException {

        public ClassificationException(String message) {
            super(message);
        }

        public ClassificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
