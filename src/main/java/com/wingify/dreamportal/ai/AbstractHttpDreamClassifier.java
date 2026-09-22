package com.wingify.dreamportal.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wingify.dreamportal.config.ConfigReader;
import com.wingify.dreamportal.model.DreamType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared HTTP, retry and answer-parsing logic for the hosted model providers.
 *
 * <p>Subclasses only supply the request they need and say where the answer sits in the response;
 * everything a flaky network call needs — timeouts, bounded retries with backoff, and strict
 * parsing of the model's answer — lives here so it is written once.
 */
public abstract class AbstractHttpDreamClassifier implements DreamClassifier {

    /**
     * The instruction sent to every provider. Constrained to a single word so the response is
     * cheap to parse and the model has no room to hedge.
     */
    protected static final String SYSTEM_PROMPT = """
            You classify dream descriptions by emotional tone for a dream journalling app.
            Answer with exactly one word: Good or Bad.
            Good means a pleasant, happy, exciting or wondrous dream.
            Bad means a frightening, stressful, sad or unpleasant dream.
            Do not explain. Do not add punctuation. Reply with the single word only.""";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LogManager.getLogger(AbstractHttpDreamClassifier.class);

    protected final HttpClient httpClient;
    protected final String apiKey;
    protected final String model;
    private final int maxAttempts;
    private final Duration timeout;

    /**
     * @param apiKeyConfigKey the config key holding this provider's API key, e.g. {@code openai.api.key}
     * @param defaultModel    the model used when {@code ai.model} is not set
     * @throws IllegalStateException when the API key is not configured
     */
    protected AbstractHttpDreamClassifier(String apiKeyConfigKey, String defaultModel) {
        this.apiKey = ConfigReader.get(apiKeyConfigKey);
        if (apiKey == null) {
            throw new IllegalStateException(
                    "No API key found for provider '" + providerName() + "'. Set " + apiKeyConfigKey
                            + " in .env, or switch ai.provider to 'rule'.");
        }
        this.model = ConfigReader.get("ai.model", defaultModel);
        this.timeout = Duration.ofSeconds(ConfigReader.getInt("ai.timeout.seconds", 30));
        this.maxAttempts = Math.max(1, ConfigReader.getInt("ai.max.attempts", 3));
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /** @return the fully formed request for this provider, including auth headers and body. */
    protected abstract HttpRequest buildRequest(String dreamName);

    /**
     * @param responseBody the raw JSON body returned by the provider
     * @return the model's answer text, before it is parsed into a {@link DreamType}
     */
    protected abstract String extractAnswer(JsonNode responseBody);

    @Override
    public DreamClassification classify(String dreamName) {
        if (dreamName == null || dreamName.isBlank()) {
            throw new ClassificationException("Cannot classify an empty dream name");
        }

        ClassificationException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String answer = callProvider(dreamName);
                DreamType type = parseVerdict(answer, dreamName);
                LOG.info("{} classified '{}' as {}", providerName(), dreamName, type);
                return new DreamClassification(dreamName, type, answer, providerName());
            } catch (ClassificationException e) {
                lastFailure = e;
                LOG.warn("Attempt {}/{} to classify '{}' failed: {}",
                        attempt, maxAttempts, dreamName, e.getMessage());
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt);
                }
            }
        }
        throw new ClassificationException(
                "Could not classify '" + dreamName + "' after " + maxAttempts + " attempts",
                lastFailure);
    }

    private String callProvider(String dreamName) {
        HttpRequest request = buildRequest(dreamName);
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ClassificationException("Network error calling " + providerName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClassificationException("Interrupted while calling " + providerName(), e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new ClassificationException(
                    "%s returned HTTP %d: %s".formatted(
                            providerName(), response.statusCode(), truncate(response.body())));
        }

        try {
            String answer = extractAnswer(MAPPER.readTree(response.body()));
            if (answer == null || answer.isBlank()) {
                throw new ClassificationException(
                        providerName() + " returned an empty answer: " + truncate(response.body()));
            }
            return answer.trim();
        } catch (IOException e) {
            throw new ClassificationException(
                    "Could not parse the response from " + providerName(), e);
        }
    }

    /**
     * Turns the model's reply into a verdict.
     *
     * <p>Kept deliberately strict: the reply must actually name one of the two classes. A model
     * that answers with something else is a real signal about the prompt, not something to
     * paper over with a default.
     */
    protected DreamType parseVerdict(String answer, String dreamName) {
        String normalised = answer.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (normalised.contains("good")) {
            return DreamType.GOOD;
        }
        if (normalised.contains("bad")) {
            return DreamType.BAD;
        }
        throw new ClassificationException(
                "%s answered '%s' for '%s', which is neither Good nor Bad"
                        .formatted(providerName(), truncate(answer), dreamName));
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Duration.ofMillis(500L * attempt).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json");
    }

    protected static String truncate(String text) {
        if (text == null) {
            return "<null>";
        }
        String single = text.replaceAll("\\s+", " ").trim();
        return single.length() <= 300 ? single : single.substring(0, 300) + "...";
    }
}
