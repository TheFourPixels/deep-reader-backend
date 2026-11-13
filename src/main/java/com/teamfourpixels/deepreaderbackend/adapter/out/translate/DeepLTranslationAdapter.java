package com.teamfourpixels.deepreaderbackend.adapter.out.translate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamfourpixels.deepreaderbackend.domain.exception.ExternalProviderException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ProviderTimeoutException;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;
import com.teamfourpixels.deepreaderbackend.infrastructure.config.RestClientConfig;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.out.TranslationPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.translator.provider", havingValue = "deepl", matchIfMissing = true)
public class DeepLTranslationAdapter implements TranslationPort {

    private static final Logger log = LoggerFactory.getLogger(DeepLTranslationAdapter.class);
    private static final String PROVIDER = "deepl";

    private final RestClient restClient;
    private final DeepReaderMetrics metrics;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Autowired
    public DeepLTranslationAdapter(
            @Value("${app.ai.deepl.api-key:mock-key:fx}") String apiKey,
            @Value("${app.ai.deepl.timeout-seconds:3}") int timeoutSeconds,
            @Qualifier("deepLRequestFactory") ClientHttpRequestFactory requestFactory,
            @Qualifier("deepLCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("deepLRetry") Retry retry,
            DeepReaderMetrics metrics) {
        String host = (apiKey != null && apiKey.endsWith(":fx"))
                ? "https://api-free.deepl.com"
                : "https://api.deepl.com";
        log.atInfo().log("DeepL adapter: host={}", host);
        this.metrics = metrics;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;

        ClientHttpRequestFactory factory = requestFactory != null
                ? requestFactory
                : RestClientConfig.createRequestFactory(Duration.ofSeconds(2), Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(host)
                .defaultHeader("Authorization", "DeepL-Auth-Key " + (apiKey != null ? apiKey : ""))
                .build();
    }

    public DeepLTranslationAdapter(
            RestClient restClient,
            CircuitBreaker circuitBreaker,
            Retry retry,
            DeepReaderMetrics metrics) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.metrics = metrics;
    }

    @Override
    public TranslationResponseDomain translate(TranslationRequestDomain request) {
        long start = System.currentTimeMillis();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("text", request.text());
        form.add("target_lang", request.targetLanguage().toUpperCase());
        if (request.sourceLanguage() != null && !request.sourceLanguage().isBlank()) {
            form.add("source_lang", request.sourceLanguage().toUpperCase());
        }

        try {
            DeepLResponse response = CircuitBreaker.decorateSupplier(circuitBreaker,
                    Retry.decorateSupplier(retry, () -> executeTranslate(form))
            ).get();

            if (response == null || response.translations() == null || response.translations().isEmpty()) {
                throw new ExternalProviderException(PROVIDER, "DeepL вернул пустой ответ");
            }

            DeepLTranslation t = response.translations().getFirst();
            long elapsed = System.currentTimeMillis() - start;
            metrics.recordTranslationDuration(PROVIDER, "ok", Duration.ofMillis(elapsed));
            metrics.recordProviderDuration(PROVIDER, elapsed, true);
            log.atInfo().log("DeepL перевод: sourceDetected={}, elapsed={}ms", t.detectedSourceLanguage(), elapsed);

            return new TranslationResponseDomain(
                    t.text(),
                    t.detectedSourceLanguage() != null ? t.detectedSourceLanguage().toLowerCase() : request.sourceLanguage(),
                    request.targetLanguage(),
                    false
            );
        } catch (CallNotPermittedException e) {
            recordFailure(start, "TRANSLATION_FAILED");
            throw e;
        } catch (ResourceAccessException e) {
            recordFailure(start, "EXTERNAL_PROVIDER_TIMEOUT");
            throw new ProviderTimeoutException(PROVIDER, "Таймаут вызова DeepL API: " + e.getMessage());
        } catch (ExternalProviderException e) {
            recordFailure(start, "TRANSLATION_FAILED");
            throw e;
        } catch (Exception e) {
            recordFailure(start, "TRANSLATION_FAILED");
            throw new ExternalProviderException(PROVIDER, "Ошибка DeepL API: " + e.getMessage(), e);
        }
    }

    private void recordFailure(long start, String errorCode) {
        long elapsed = System.currentTimeMillis() - start;
        metrics.recordTranslationDuration(PROVIDER, "error", Duration.ofMillis(elapsed));
        metrics.recordProviderDuration(PROVIDER, elapsed, false);
        metrics.recordError(errorCode, PROVIDER);
    }

    private DeepLResponse executeTranslate(MultiValueMap<String, String> form) {
        try {
            return restClient.post()
                    .uri("/v2/translate")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(DeepLResponse.class);
        } catch (ResourceAccessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new ExternalProviderException(PROVIDER, "DeepL HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ExternalProviderException(PROVIDER, "Ошибка DeepL API: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DeepLResponse(List<DeepLTranslation> translations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DeepLTranslation(
            @JsonProperty("detected_source_language") String detectedSourceLanguage,
            String text
    ) {}
}
