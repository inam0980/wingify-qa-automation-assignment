package com.wingify.dreamportal.ai;

import com.wingify.dreamportal.config.ConfigReader;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Picks the classifier named by {@code ai.provider}.
 *
 * <p>If a hosted provider is requested but its API key is missing, the factory falls back to the
 * local rule-based classifier rather than failing the run — a reviewer cloning the repo without
 * keys still sees the bonus tests execute. The fallback is logged loudly, and can be turned off
 * with {@code ai.fallback.to.rules=false} when a run must genuinely exercise a real model.
 */
public final class DreamClassifierFactory {

    private static final Logger LOG = LogManager.getLogger(DreamClassifierFactory.class);

    private DreamClassifierFactory() {
    }

    /** @return the classifier for the configured provider, never {@code null}. */
    public static DreamClassifier create() {
        String provider = ConfigReader.get("ai.provider", "rule").toLowerCase(Locale.ROOT);
        boolean allowFallback = ConfigReader.getBoolean("ai.fallback.to.rules", true);

        if (provider.equals("rule") || provider.equals("local") || provider.equals("none")) {
            LOG.info("AI provider: local rule-based classifier (no API key required)");
            return new RuleBasedDreamClassifier();
        }

        try {
            DreamClassifier classifier = switch (provider) {
                case "openai" -> new OpenAiDreamClassifier();
                case "groq" -> new GroqDreamClassifier();
                case "gemini", "google" -> new GeminiDreamClassifier();
                default -> throw new IllegalArgumentException(
                        "Unknown ai.provider '" + provider
                                + "'. Supported: rule, openai, groq, gemini.");
            };
            LOG.info("AI provider: {}", classifier.providerName());
            return classifier;
        } catch (IllegalStateException e) {
            if (!allowFallback) {
                throw e;
            }
            LOG.warn("Provider '{}' is not usable ({}). Falling back to the local rule-based "
                    + "classifier; set ai.fallback.to.rules=false to fail instead.",
                    provider, e.getMessage());
            return new RuleBasedDreamClassifier();
        }
    }
}
