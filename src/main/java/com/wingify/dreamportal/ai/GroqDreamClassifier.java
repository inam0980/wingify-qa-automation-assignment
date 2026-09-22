package com.wingify.dreamportal.ai;

import com.wingify.dreamportal.config.ConfigReader;

/**
 * Classifies dreams using Groq, which serves open-source models (Llama, Mixtral, Gemma) behind an
 * OpenAI-compatible endpoint. Groq's free tier makes this the cheapest way for a reviewer to run
 * the bonus tests against a real model.
 */
public class GroqDreamClassifier extends OpenAiCompatibleDreamClassifier {

    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final String DEFAULT_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    public GroqDreamClassifier() {
        super("groq.api.key", DEFAULT_MODEL);
    }

    @Override
    public String providerName() {
        return "Groq (" + ConfigReader.get("ai.model", DEFAULT_MODEL) + ")";
    }

    @Override
    protected String endpoint() {
        return ConfigReader.get("groq.api.url", DEFAULT_ENDPOINT);
    }
}
