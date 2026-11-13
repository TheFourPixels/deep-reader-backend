package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.adapter.out.llm.OpenAiLlmAdapter;
import com.teamfourpixels.deepreaderbackend.domain.exception.EmptyAiResponseException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ExternalProviderException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ProviderTimeoutException;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainResponseDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.PhenomenonType;
import com.teamfourpixels.deepreaderbackend.domain.model.StylisticColoring;
import com.teamfourpixels.deepreaderbackend.domain.model.SurroundingContext;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiLlmAdapterTest {

    private MockRestServiceServer mockServer;
    private OpenAiLlmAdapter adapter;
    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private final String baseUrl = "https://api.openai.com/v1";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        ResilienceConfig resilienceConfig = new ResilienceConfig();
        circuitBreaker = resilienceConfig.openAiCircuitBreaker();
        retry = resilienceConfig.openAiRetry();

        ObjectMapper objectMapper = JsonMapper.builder().build();
        DeepReaderMetrics metrics = new DeepReaderMetrics(new SimpleMeterRegistry());

        adapter = new OpenAiLlmAdapter(
                restClient,
                circuitBreaker,
                retry,
                objectMapper,
                metrics,
                "gpt-4o-mini"
        );
    }

    private ExplainRequestDomain sampleRequest() {
        return new ExplainRequestDomain(
                "1984-george-orwell",
                "clocks were striking thirteen",
                "It was a bright cold day, and clocks were striking thirteen.",
                new SurroundingContext(List.of("Sentence before"), List.of("Sentence after"), "Current paragraph", "Chapter 1"),
                "ru"
        );
    }

    @Test
    void testExplainSuccess200() {
        String jsonPayload = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"contextual_meaning\\":\\"Нарушение привычного порядка\\",\\"phenomenon_type\\":\\"metaphor\\",\\"explanation\\":\\"Удар 13 символизирует искажение реальности\\",\\"stylistic_coloring\\":\\"literary\\",\\"spoiler_alert\\":false}"
                      }
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 50,
                    "completion_tokens": 30
                  }
                }
                """;

        mockServer.expect(requestTo(baseUrl + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.response_format.type", is("json_object")))
                .andExpect(jsonPath("$.messages[1].content", containsString("[Before]: Sentence before")))
                .andExpect(jsonPath("$.messages[1].content", containsString("[Current paragraph]: Current paragraph")))
                .andExpect(jsonPath("$.messages[1].content", containsString("[After]: Sentence after")))
                .andExpect(jsonPath("$.messages[1].content", containsString("[Chapter context]: Chapter 1")))
                .andRespond(withSuccess(jsonPayload, MediaType.APPLICATION_JSON));

        ExplainResponseDomain response = adapter.explain(sampleRequest());

        assertNotNull(response);
        assertEquals("clocks were striking thirteen", response.selectedText());
        assertEquals("Нарушение привычного порядка", response.contextualMeaning());
        assertEquals(PhenomenonType.METAPHOR, response.phenomenonType());
        assertEquals("Удар 13 символизирует искажение реальности", response.explanation());
        assertEquals(StylisticColoring.LITERARY, response.stylisticColoring());
        assertFalse(response.spoilerAlert());
        mockServer.verify();
    }

    @Test
    void testExplainEmptyChoicesThrows422EmptyAiResponse() {
        String emptyPayload = """
                {
                  "choices": []
                }
                """;

        mockServer.expect(requestTo(baseUrl + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(emptyPayload, MediaType.APPLICATION_JSON));

        assertThrows(EmptyAiResponseException.class, () -> adapter.explain(sampleRequest()));
        mockServer.verify();
    }

    @Test
    void testExplainTimeoutThrows504ProviderTimeoutException() {

        mockServer.expect(requestTo(baseUrl + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.response_format.type", is("json_object")))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"));
                });
        mockServer.expect(requestTo(baseUrl + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.response_format.type", is("json_object")))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"));
                });

        ProviderTimeoutException ex = assertThrows(ProviderTimeoutException.class, () -> adapter.explain(sampleRequest()));
        assertEquals("openai", ex.getProvider());
        mockServer.verify();
    }

    @Test
    void testExplain500ServerErrorThrows502ExternalProviderException() {

        mockServer.expect(requestTo(baseUrl + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.response_format.type", is("json_object")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error"));
        mockServer.expect(requestTo(baseUrl + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.response_format.type", is("json_object")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error"));

        ExternalProviderException ex = assertThrows(ExternalProviderException.class, () -> adapter.explain(sampleRequest()));
        assertEquals("openai", ex.getProvider());
        mockServer.verify();
    }

    @Test
    void testCircuitBreakerOpenThrowsCallNotPermittedException() {
        circuitBreaker.transitionToOpenState();

        CallNotPermittedException ex = assertThrows(CallNotPermittedException.class, () -> adapter.explain(sampleRequest()));
        assertEquals("openai", ex.getCausingCircuitBreakerName());
    }
}
