package com.wingify.dreamportal.ai;

import com.wingify.dreamportal.config.ConfigReader;

/** Classifies dreams using the OpenAI chat-completions API. */
public class OpenAiDreamClassifier extends OpenAiCompatibleDreamClassifier {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    public OpenAiDreamClassifier() {
        super("openai.api.key", DEFAULT_MODEL);
    }

    @Override
    public String providerName() {
        return "OpenAI (" + ConfigReader.get("ai.model", DEFAULT_MODEL) + ")";
    }

    @Override
    protected String endpoint() {
        return ConfigReader.get("openai.api.url", DEFAULT_ENDPOINT);
    }
}
