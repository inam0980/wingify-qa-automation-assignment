package com.wingify.dreamportal.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpRequest;

/**
 * Base for every provider that speaks the OpenAI {@code /chat/completions} shape — OpenAI itself,
 * Groq, Together, OpenRouter, a local Ollama or vLLM server, and so on.
 *
 * <p>Adding one of those is then a subclass with an endpoint, a key and a default model.
 */
public abstract class OpenAiCompatibleDreamClassifier extends AbstractHttpDreamClassifier {

    protected OpenAiCompatibleDreamClassifier(String apiKeyConfigKey, String defaultModel) {
        super(apiKeyConfigKey, defaultModel);
    }

    /** @return the full chat-completions URL for this provider. */
    protected abstract String endpoint();

    @Override
    protected HttpRequest buildRequest(String dreamName) {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("model", model);
        // Deterministic output: the same dream name must classify the same way on every run,
        // otherwise the assertion is flaky by construction.
        payload.put("temperature", 0);
        payload.put("max_tokens", 16);

        ArrayNode messages = payload.putArray("messages");

        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", "Dream: " + dreamName);

        return baseRequest(endpoint())
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
    }

    @Override
    protected String extractAnswer(JsonNode responseBody) {
        JsonNode content = responseBody.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new ClassificationException(
                    providerName() + " response had no choices[0].message.content: "
                            + truncate(responseBody.toString()));
        }
        return content.asText();
    }
}
