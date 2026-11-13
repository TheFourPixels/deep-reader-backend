package com.teamfourpixels.deepreaderbackend.adapter.out.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import com.teamfourpixels.deepreaderbackend.domain.exception.EmptyAiResponseException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ExternalProviderException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ProviderTimeoutException;
import com.teamfourpixels.deepreaderbackend.domain.model.*;
import com.teamfourpixels.deepreaderbackend.infrastructure.config.RestClientConfig;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.out.LlmPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.llm.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiLlmAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmAdapter.class);
    private static final String PROVIDER = "openai";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DeepReaderMetrics metrics;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final String model;

    @Autowired
    public OpenAiLlmAdapter(
            @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.ai.openai.api-key:mock-key}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.openai.timeout-seconds:12}") int timeoutSeconds,
            @Qualifier("openAiRequestFactory") ClientHttpRequestFactory requestFactory,
            @Qualifier("openAiCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("openAiRetry") Retry retry,
            ObjectMapper objectMapper,
            DeepReaderMetrics metrics) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;

        ClientHttpRequestFactory factory = requestFactory != null
                ? requestFactory
                : RestClientConfig.createRequestFactory(Duration.ofSeconds(5), Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (apiKey != null ? apiKey : ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public OpenAiLlmAdapter(
            RestClient restClient,
            CircuitBreaker circuitBreaker,
            Retry retry,
            ObjectMapper objectMapper,
            DeepReaderMetrics metrics,
            String model) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.model = model;
    }

    @Override
    public ExplainResponseDomain explain(ExplainRequestDomain request) {
        long start = System.currentTimeMillis();
        String surroundingCtx = buildSurroundingContextText(request);
        String userPrompt = PromptTemplates.buildUserPrompt(
                request.selectedText(),
                request.currentSentence(),
                surroundingCtx,
                request.targetLanguage());

        ChatRequest chatRequest = new ChatRequest(
                model,
                List.of(
                        new Message("system", PromptTemplates.SYSTEM_PROMPT),
                        new Message("user", userPrompt)
                ),
                new ResponseFormat("json_object"),
                0.3
        );

        try {
            ChatResponse chatResponse = CircuitBreaker.decorateSupplier(circuitBreaker,
                    Retry.decorateSupplier(retry, () -> executeChatCompletion(chatRequest))
            ).get();

            if (chatResponse == null
                    || chatResponse.choices() == null
                    || chatResponse.choices().isEmpty()
                    || chatResponse.choices().getFirst().message() == null) {
                throw new EmptyAiResponseException("Провайдер AI вернул пустой ответ. Пожалуйста, повторите запрос.");
            }

            String rawContent = chatResponse.choices().getFirst().message().content();
            if (rawContent == null || rawContent.isBlank()) {
                throw new EmptyAiResponseException("Провайдер AI вернул пустой ответ. Пожалуйста, повторите запрос.");
            }

            if (chatResponse.usage() != null) {
                metrics.recordTokens(model, chatResponse.usage().promptTokens(), chatResponse.usage().completionTokens());
            }

            long elapsed = System.currentTimeMillis() - start;
            metrics.recordLlmDuration(PROVIDER, model, "ok", Duration.ofMillis(elapsed));
            metrics.recordProviderDuration(PROVIDER, elapsed, true);
            log.atInfo().log("OpenAI response получен: model={}, elapsed={}ms", model, elapsed);

            return parseResponse(rawContent, request.selectedText());

        } catch (EmptyAiResponseException e) {
            recordFailure(start, "EMPTY_AI_RESPONSE");
            throw e;
        } catch (CallNotPermittedException e) {
            recordFailure(start, "LLM_PROVIDER_ERROR");
            throw e;
        } catch (ResourceAccessException e) {
            recordFailure(start, "EXTERNAL_PROVIDER_TIMEOUT");
            throw new ProviderTimeoutException(PROVIDER, "Таймаут вызова OpenAI API: " + e.getMessage());
        } catch (ExternalProviderException e) {
            recordFailure(start, "LLM_PROVIDER_ERROR");
            throw e;
        } catch (Exception e) {
            recordFailure(start, "LLM_PROVIDER_ERROR");
            throw new ExternalProviderException(PROVIDER, "Ошибка вызова OpenAI API: " + e.getMessage(), e);
        }
    }

    private void recordFailure(long start, String errorCode) {
        long elapsed = System.currentTimeMillis() - start;
        metrics.recordLlmDuration(PROVIDER, model, "error", Duration.ofMillis(elapsed));
        metrics.recordProviderDuration(PROVIDER, elapsed, false);
        metrics.recordError(errorCode, PROVIDER);
    }

    private ChatResponse executeChatCompletion(ChatRequest chatRequest) {
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .body(chatRequest)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (ResourceAccessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new ExternalProviderException(PROVIDER, "OpenAI HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ExternalProviderException(PROVIDER, "Ошибка вызова OpenAI API: " + e.getMessage(), e);
        }
    }

    private ExplainResponseDomain parseResponse(String rawJson, String selectedText) {
        try {
            LlmJsonResponse parsed = objectMapper.readValue(rawJson, LlmJsonResponse.class);
            if (parsed.explanation() == null || parsed.explanation().isBlank()
                    || parsed.contextualMeaning() == null || parsed.contextualMeaning().isBlank()) {
                throw new EmptyAiResponseException("Провайдер AI вернул пустой ответ. Пожалуйста, повторите запрос.");
            }
            return new ExplainResponseDomain(
                    selectedText,
                    parsed.contextualMeaning(),
                    parsePhenomenon(parsed.phenomenonType()),
                    parsed.explanation(),
                    parseColoring(parsed.stylisticColoring()),
                    parsed.spoilerAlert(),
                    false,
                    0L
            );
        } catch (EmptyAiResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalProviderException(PROVIDER, "Не удалось распарсить JSON ответ OpenAI: " + e.getMessage(), e);
        }
    }

    private PhenomenonType parsePhenomenon(String value) {
        if (value == null) return PhenomenonType.LITERAL_PHRASE;
        try {
            return PhenomenonType.valueOf(value.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return PhenomenonType.LITERAL_PHRASE;
        }
    }

    private StylisticColoring parseColoring(String value) {
        if (value == null) return StylisticColoring.NEUTRAL;
        try {
            return StylisticColoring.valueOf(value.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return StylisticColoring.NEUTRAL;
        }
    }

    private String buildSurroundingContextText(ExplainRequestDomain request) {
        SurroundingContext ctx = request.surroundingContext();
        StringBuilder sb = new StringBuilder();
        if (!ctx.sentencesBefore().isEmpty()) {
            sb.append("[Before]: ").append(String.join(" ", ctx.sentencesBefore())).append("\n");
        }
        if (ctx.paragraph() != null && !ctx.paragraph().isBlank()) {
            sb.append("[Current paragraph]: ").append(ctx.paragraph()).append("\n");
        }
        if (!ctx.sentencesAfter().isEmpty()) {
            sb.append("[After]: ").append(String.join(" ", ctx.sentencesAfter())).append("\n");
        }
        if (ctx.chapterContext() != null && !ctx.chapterContext().isBlank()) {
            sb.append("[Chapter context]: ").append(ctx.chapterContext()).append("\n");
        }
        return sb.toString().strip();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatRequest(
            String model,
            List<Message> messages,
            @JsonProperty("response_format") ResponseFormat responseFormat,
            double temperature) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseFormat(String type) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmJsonResponse(
            @JsonProperty("contextual_meaning") String contextualMeaning,
            @JsonProperty("phenomenon_type") String phenomenonType,
            @JsonProperty("explanation") String explanation,
            @JsonProperty("stylistic_coloring") String stylisticColoring,
            @JsonProperty("spoiler_alert") boolean spoilerAlert) {}
}
