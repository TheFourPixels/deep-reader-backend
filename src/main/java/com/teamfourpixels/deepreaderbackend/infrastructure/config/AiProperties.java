package com.teamfourpixels.deepreaderbackend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String llmProvider = "openai";
    private String translatorProvider = "deepl";
    private int maxContextTokens = 1000;
    private String promptVersion = "v1.0";
    private OpenAi openai = new OpenAi();
    private Deepl deepl = new Deepl();

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getTranslatorProvider() {
        return translatorProvider;
    }

    public void setTranslatorProvider(String translatorProvider) {
        this.translatorProvider = translatorProvider;
    }

    public int getMaxContextTokens() {
        return maxContextTokens;
    }

    public void setMaxContextTokens(int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public Deepl getDeepl() {
        return deepl;
    }

    public void setDeepl(Deepl deepl) {
        this.deepl = deepl;
    }

    public static class OpenAi {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private int timeoutSeconds = 12;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Duration getTimeout() {
            return Duration.ofSeconds(timeoutSeconds);
        }
    }

    public static class Deepl {
        private String apiKey = "";
        private int timeoutSeconds = 3;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Duration getTimeout() {
            return Duration.ofSeconds(timeoutSeconds);
        }
    }
}
