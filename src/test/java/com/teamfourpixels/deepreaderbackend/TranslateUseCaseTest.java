package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.application.TranslateUseCaseImpl;
import com.teamfourpixels.deepreaderbackend.domain.model.CacheType;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;
import com.teamfourpixels.deepreaderbackend.domain.service.CacheKeyGenerator;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.out.CachePort;
import com.teamfourpixels.deepreaderbackend.port.out.TranslationPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslateUseCaseTest {

    @Mock
    private CachePort cachePort;

    @Mock
    private TranslationPort translationPort;

    private CacheKeyGenerator keyGenerator;
    private DeepReaderMetrics metrics;
    private ObjectMapper objectMapper;
    private TranslateUseCaseImpl translateUseCase;

    @BeforeEach
    void setUp() {
        keyGenerator = new CacheKeyGenerator("v1.0");
        metrics = new DeepReaderMetrics(new SimpleMeterRegistry());
        objectMapper = JsonMapper.builder().build();
        translateUseCase = new TranslateUseCaseImpl(keyGenerator, cachePort, translationPort, metrics, objectMapper, 90);
    }

    @Test
    void testTranslateCacheHitWithJsonPreservesDetectedSourceLanguage() {
        TranslationRequestDomain req = new TranslationRequestDomain("Hello world", null, "ru");
        String cachedJson = "{\"translated_text\":\"Привет мир\",\"source_language\":\"en\"}";

        when(cachePort.get(anyString(), eq("translate"))).thenReturn(Optional.of(cachedJson));

        TranslationResponseDomain result = translateUseCase.translate(req);

        assertTrue(result.cached());
        assertEquals("Привет мир", result.translatedText());
        assertEquals("en", result.sourceLanguage());
        assertEquals("ru", result.targetLanguage());
        verify(translationPort, never()).translate(any());
    }

    @Test
    void testTranslateCacheHitFallbackForPlainText() {
        TranslationRequestDomain req = new TranslationRequestDomain("Hello world", "en", "ru");

        when(cachePort.get(anyString(), eq("translate"))).thenReturn(Optional.of("Привет мир"));

        TranslationResponseDomain result = translateUseCase.translate(req);

        assertTrue(result.cached());
        assertEquals("Привет мир", result.translatedText());
        assertEquals("en", result.sourceLanguage());
        assertEquals("ru", result.targetLanguage());
        verify(translationPort, never()).translate(any());
    }

    @Test
    void testTranslateCacheMissCallsTranslationPortAndCachesJson() {
        TranslationRequestDomain req = new TranslationRequestDomain("Hello world", null, "ru");
        TranslationResponseDomain response = new TranslationResponseDomain("Привет мир", "en", "ru", false);

        when(cachePort.get(anyString(), eq("translate"))).thenReturn(Optional.empty());
        when(translationPort.translate(any())).thenReturn(response);

        TranslationResponseDomain result = translateUseCase.translate(req);

        assertFalse(result.cached());
        assertEquals("Привет мир", result.translatedText());
        assertEquals("en", result.sourceLanguage());
        verify(translationPort, times(1)).translate(any());
        verify(cachePort, times(1)).put(anyString(), contains("Привет мир"), eq(CacheType.TRANSLATE), any(Duration.class));
    }
}
