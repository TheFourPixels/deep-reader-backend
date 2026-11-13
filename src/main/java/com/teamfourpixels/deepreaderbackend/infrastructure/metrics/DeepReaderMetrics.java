package com.teamfourpixels.deepreaderbackend.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DeepReaderMetrics {

    private final MeterRegistry meterRegistry;

    public DeepReaderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementRequest(String endpoint) {
        Counter.builder("ai.requests.total")
                .description("Total incoming AI proxy requests")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }

    public void incrementRequest(String endpoint, String status) {
        Counter.builder("ai.requests.total")
                .description("Total incoming AI proxy requests")
                .tag("endpoint", endpoint)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheRequest(String action, String level, String result) {
        Counter.builder("ai.cache.requests.total")
                .description("AI cache hit/miss statistics")
                .tag("action", action)
                .tag("level", level)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public void recordLlmDuration(String provider, String model, String status, Duration duration) {
        Timer.builder("ai.llm.duration.seconds")
                .description("LLM external API latency")
                .tag("provider", provider)
                .tag("model", model)
                .tag("status", status)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);
    }

    public void recordTranslationDuration(String provider, String status, Duration duration) {
        Timer.builder("ai.translation.duration.seconds")
                .description("Translation service duration")
                .tag("provider", provider)
                .tag("status", status)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);
    }

    public void recordProviderDuration(String provider, long durationMs, boolean success) {
        recordProviderDuration(provider, Duration.ofMillis(durationMs), success);
    }

    public void recordProviderDuration(String provider, Duration duration, boolean success) {
        String status = success ? "ok" : "error";
        Timer.builder("ai.provider.latency.seconds")
                .description("External AI/Translation provider latency")
                .tag("provider", provider)
                .tag("status", status)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);

        if ("deepl".equalsIgnoreCase(provider)) {
            recordTranslationDuration(provider, status, duration);
        }
    }

    public void recordTokens(String model, int promptTokens, int completionTokens) {
        if (promptTokens > 0) {
            Counter.builder("ai.tokens.consumed.total")
                    .description("Total LLM tokens consumed")
                    .tag("model", model)
                    .tag("token_type", "prompt")
                    .register(meterRegistry)
                    .increment(promptTokens);
        }
        if (completionTokens > 0) {
            Counter.builder("ai.tokens.consumed.total")
                    .description("Total LLM tokens consumed")
                    .tag("model", model)
                    .tag("token_type", "completion")
                    .register(meterRegistry)
                    .increment(completionTokens);
        }
    }

    public void recordTokens(String provider, String model, int promptTokens, int completionTokens) {
        recordTokens(model, promptTokens, completionTokens);
    }

    public void recordError(String errorCode, String provider) {
        Counter.builder("ai.errors.total")
                .description("Total AI proxy errors by code and provider")
                .tag("error_code", errorCode)
                .tag("provider", provider != null ? provider : "unknown")
                .register(meterRegistry)
                .increment();
    }
}
