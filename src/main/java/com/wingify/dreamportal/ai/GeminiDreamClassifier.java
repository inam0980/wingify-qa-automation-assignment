package com.wingify.dreamportal.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wingify.dreamportal.config.ConfigReader;
import java.net.http.HttpRequest;

/**
 * Classifies dreams using the Google Gemini generative-language API.
 *
 * <p>Gemini uses its own request shape rather than the OpenAI one, so it overrides the request
 * builder and answer extraction instead of extending the compatible base class.
 */
public class GeminiDreamClassifier extends AbstractHttpDreamClassifier {

    private static final String DEFAULT_MODEL = "gemini-2.0-flash";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    public GeminiDreamClassifier() {
        super("gemini.api.key", DEFAULT_MODEL);
    }

    @Override
    public String providerName() {
        return "Gemini (" + ConfigReader.get("ai.model", DEFAULT_MODEL) + ")";
    }

    @Override
    protected HttpRequest buildRequest(String dreamName) {
        ObjectNode payload = MAPPER.createObjectNode();

        ObjectNode systemInstruction = payload.putObject("system_instruction");
        systemInstruction.putArray("parts").addObject().put("text", SYSTEM_PROMPT);

        payload.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", "Dream: " + dreamName);

        ObjectNode generationConfig = payload.putObject("generationConfig");
        generationConfig.put("temperature", 0);
        generationConfig.put("maxOutputTokens", 16);

        String baseUrl = ConfigReader.get("gemini.api.url", DEFAULT_BASE_URL);
        String url = "%s/%s:generateContent".formatted(baseUrl, model);

        return baseRequest(url)
                // Passed as a header rather than a query parameter so the key never lands in a log
                // line or an HTTP access log.
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
    }

    @Override
    protected String extractAnswer(JsonNode responseBody) {
        JsonNode text = responseBody
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text");

        if (text.isMissingNode() || text.isNull()) {
            String blockReason = responseBody.path("promptFeedback").path("blockReason").asText("");
            if (!blockReason.isEmpty()) {
                throw new ClassificationException(
                        "Gemini blocked the prompt (reason: " + blockReason + ")");
            }
            throw new ClassificationException(
                    "Gemini response had no candidate text: " + truncate(responseBody.toString()));
        }
        return text.asText();
    }
}
