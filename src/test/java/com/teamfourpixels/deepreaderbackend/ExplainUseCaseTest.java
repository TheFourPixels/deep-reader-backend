package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.application.ExplainUseCaseImpl;
import com.teamfourpixels.deepreaderbackend.domain.exception.EmptyAiResponseException;
import com.teamfourpixels.deepreaderbackend.domain.model.*;
import com.teamfourpixels.deepreaderbackend.domain.service.CacheKeyGenerator;
import com.teamfourpixels.deepreaderbackend.domain.service.ContextTruncationService;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.out.CachePort;
import com.teamfourpixels.deepreaderbackend.port.out.LlmPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExplainUseCaseTest {

    @Mock
    private CachePort cachePort;

    @Mock
    private LlmPort llmPort;

    private ContextTruncationService truncationService;
    private CacheKeyGenerator keyGenerator;
    private SimpleMeterRegistry meterRegistry;
    private DeepReaderMetrics metrics;
    private ObjectMapper objectMapper;
    private ExplainUseCaseImpl explainUseCase;

    @BeforeEach
    void setUp() {
        truncationService = new ContextTruncationService(1000);
        keyGenerator = new CacheKeyGenerator("v1.0");
        meterRegistry = new SimpleMeterRegistry();
        metrics = new DeepReaderMetrics(meterRegistry);
        objectMapper = JsonMapper.builder().build();

        explainUseCase = new ExplainUseCaseImpl(
                truncationService,
                keyGenerator,
                cachePort,
                llmPort,
                objectMapper,
                metrics,
                30
        );
    }

    @Test
    void testExplainCacheHitReturnsCachedResultWithoutCallingLlm() throws Exception {
        ExplainRequestDomain req = new ExplainRequestDomain(
                "book-1",
                "clocks were striking thirteen",
                "It was a bright cold day, and clocks were striking thirteen.",
                new SurroundingContext(List.of(), List.of(), null, null),
                "ru"
        );

        ExplainResponseDomain cachedDomain = new ExplainResponseDomain(
                "clocks were striking thirteen",
                "Часы били тринадцать",
                PhenomenonType.METAPHOR,
                "Объяснение метафоры",
                StylisticColoring.LITERARY,
                false,
                false,
                0L
        );
        String cachedJson = objectMapper.writeValueAsString(cachedDomain);

        when(cachePort.get(anyString(), anyString())).thenReturn(Optional.of(cachedJson));

        ExplainResponseDomain result = explainUseCase.explain(req);

        assertTrue(result.cached());
        assertEquals("clocks were striking thirteen", result.selectedText());
        assertEquals(PhenomenonType.METAPHOR, result.phenomenonType());
        verify(llmPort, never()).explain(any());
        assertEquals(1.0, meterRegistry.get("ai.requests.total").tag("endpoint", "explain").counter().count());
    }

    @Test
    void testExplainCacheMissCallsLlmAndSavesToCache() {
        ExplainRequestDomain req = new ExplainRequestDomain(
                "book-1",
                "clocks were striking thirteen",
                "It was a bright cold day, and clocks were striking thirteen.",
                new SurroundingContext(List.of(), List.of(), null, null),
                "ru"
        );

        ExplainResponseDomain llmResponse = new ExplainResponseDomain(
                "clocks were striking thirteen",
                "Смысловой эквивалент",
                PhenomenonType.METAPHOR,
                "Подробное объяснение",
                StylisticColoring.LITERARY,
                false,
                false,
                100L
        );

        when(cachePort.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(llmPort.explain(any())).thenReturn(llmResponse);

        ExplainResponseDomain result = explainUseCase.explain(req);

        assertFalse(result.cached());
        assertEquals("clocks were striking thirteen", result.selectedText());
        verify(llmPort, times(1)).explain(any());
        verify(cachePort, times(1)).put(anyString(), anyString(), eq(CacheType.EXPLAIN), any(Duration.class));
        assertEquals(1.0, meterRegistry.get("ai.requests.total").tag("endpoint", "explain").counter().count());
    }

    @Test
    void testExplainThrowsWhenLlmReturnsEmptyExplanation() {
        ExplainRequestDomain req = new ExplainRequestDomain(
                "book-1",
                "phrase",
                "This is phrase.",
                new SurroundingContext(List.of(), List.of(), null, null),
                "ru"
        );

        ExplainResponseDomain emptyExplanation = new ExplainResponseDomain(
                "phrase", "meaning", PhenomenonType.LITERAL_PHRASE, "   ",
                StylisticColoring.NEUTRAL, false, false, 0L
        );

        when(cachePort.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(llmPort.explain(any())).thenReturn(emptyExplanation);

        assertThrows(EmptyAiResponseException.class, () -> explainUseCase.explain(req));
        assertEquals(1.0, meterRegistry.get("ai.requests.total").tag("endpoint", "explain").counter().count());
    }

    @Test
    void testExplainThrowsWhenLlmReturnsEmptyContextualMeaning() {
        ExplainRequestDomain req = new ExplainRequestDomain(
                "book-1",
                "phrase",
                "This is phrase.",
                new SurroundingContext(List.of(), List.of(), null, null),
                "ru"
        );

        ExplainResponseDomain emptyMeaning = new ExplainResponseDomain(
                "phrase", "   ", PhenomenonType.LITERAL_PHRASE, "Valid explanation",
                StylisticColoring.NEUTRAL, false, false, 0L
        );

        when(cachePort.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(llmPort.explain(any())).thenReturn(emptyMeaning);

        assertThrows(EmptyAiResponseException.class, () -> explainUseCase.explain(req));
        assertEquals(1.0, meterRegistry.get("ai.requests.total").tag("endpoint", "explain").counter().count());
    }
}
