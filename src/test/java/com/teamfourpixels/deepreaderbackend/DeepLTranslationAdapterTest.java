package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.adapter.out.translate.DeepLTranslationAdapter;
import com.teamfourpixels.deepreaderbackend.domain.exception.ExternalProviderException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ProviderTimeoutException;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;
import com.teamfourpixels.deepreaderbackend.infrastructure.config.ResilienceConfig;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DeepLTranslationAdapterTest {

    private MockRestServiceServer mockServer;
    private DeepLTranslationAdapter adapter;
    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private DeepReaderMetrics metrics;

    private final String baseUrl = "https://api-free.deepl.com";

    @BeforeEach
    void setUp() {
        ResilienceConfig resilienceConfig = new ResilienceConfig();
        circuitBreaker = resilienceConfig.deepLCircuitBreaker();
        retry = resilienceConfig.deepLRetry();
        metrics = new DeepReaderMetrics(new SimpleMeterRegistry());

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        adapter = new DeepLTranslationAdapter(restClient, circuitBreaker, retry, metrics);
    }

    @Test
    void testTranslateSuccess200() {
        String jsonPayload = """
                {
                  "translations": [
                    {
                      "detected_source_language": "EN",
                      "text": "часы били тринадцать"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(baseUrl + "/v2/translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andRespond(withSuccess(jsonPayload, MediaType.APPLICATION_JSON));

        TranslationRequestDomain request = new TranslationRequestDomain("clocks were striking thirteen", null, "ru");
        TranslationResponseDomain response = adapter.translate(request);

        assertNotNull(response);
        assertEquals("часы били тринадцать", response.translatedText());
        assertEquals("en", response.sourceLanguage());
        assertEquals("ru", response.targetLanguage());
        assertFalse(response.cached());
        mockServer.verify();
    }

    @Test
    void testTranslateEmptyResponseThrows502ExternalProviderException() {
        String emptyPayload = """
                {
                  "translations": []
                }
                """;

        mockServer.expect(requestTo(baseUrl + "/v2/translate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(emptyPayload, MediaType.APPLICATION_JSON));

        TranslationRequestDomain request = new TranslationRequestDomain("hello", "en", "ru");
        assertThrows(ExternalProviderException.class, () -> adapter.translate(request));
        mockServer.verify();
    }

    @Test
    void testTranslateTimeoutThrows504ProviderTimeoutException() {

        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo(baseUrl + "/v2/translate"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(request -> {
                        throw new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"));
                    });
        }

        TranslationRequestDomain request = new TranslationRequestDomain("hello", "en", "ru");
        ProviderTimeoutException ex = assertThrows(ProviderTimeoutException.class, () -> adapter.translate(request));
        assertEquals("deepl", ex.getProvider());
        mockServer.verify();
    }

    @Test
    void testTranslate500ServerErrorThrows502ExternalProviderException() {

        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo(baseUrl + "/v2/translate"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("DeepL Server Error"));
        }

        TranslationRequestDomain request = new TranslationRequestDomain("hello", "en", "ru");
        ExternalProviderException ex = assertThrows(ExternalProviderException.class, () -> adapter.translate(request));
        assertEquals("deepl", ex.getProvider());
        mockServer.verify();
    }

    @Test
    void testCircuitBreakerOpenThrowsCallNotPermittedException() {
        circuitBreaker.transitionToOpenState();

        TranslationRequestDomain request = new TranslationRequestDomain("hello", "en", "ru");
        CallNotPermittedException ex = assertThrows(CallNotPermittedException.class, () -> adapter.translate(request));
        assertEquals("deepl", ex.getCausingCircuitBreakerName());
    }
}
