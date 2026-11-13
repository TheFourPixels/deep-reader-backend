package com.teamfourpixels.deepreaderbackend.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamfourpixels.deepreaderbackend.domain.model.CacheType;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;
import com.teamfourpixels.deepreaderbackend.domain.service.CacheKeyGenerator;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.in.TranslateUseCase;
import com.teamfourpixels.deepreaderbackend.port.out.CachePort;
import com.teamfourpixels.deepreaderbackend.port.out.TranslationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
public class TranslateUseCaseImpl implements TranslateUseCase {

    private static final Logger log = LoggerFactory.getLogger(TranslateUseCaseImpl.class);

    private final CacheKeyGenerator keyGenerator;
    private final CachePort cache;
    private final TranslationPort translator;
    private final DeepReaderMetrics metrics;
    private final ObjectMapper objectMapper;
    private final Duration translateCacheTtl;

    public TranslateUseCaseImpl(CacheKeyGenerator keyGenerator,
                                CachePort cache,
                                TranslationPort translator,
                                DeepReaderMetrics metrics,
                                ObjectMapper objectMapper,
                                @Value("${app.cache.translate-ttl-days:90}") int translateTtlDays) {
        this.keyGenerator = keyGenerator;
        this.cache = cache;
        this.translator = translator;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.translateCacheTtl = Duration.ofDays(translateTtlDays);
    }

    @Override
    public TranslationResponseDomain translate(TranslationRequestDomain request) {
        metrics.incrementRequest("translate");

        String cacheKey = keyGenerator.forTranslate(
                request.text(), request.sourceLanguage(), request.targetLanguage());

        Optional<String> cached = cache.get(cacheKey, "translate");
        if (cached.isPresent()) {
            log.atInfo().log("Cache hit для translate: key={}", cacheKey);
            String cachedStr = cached.get();
            String translatedText;
            String sourceLanguage;

            try {
                TranslationCacheEntry entry = objectMapper.readValue(cachedStr, TranslationCacheEntry.class);
                translatedText = entry.translatedText();
                sourceLanguage = entry.sourceLanguage() != null ? entry.sourceLanguage() : request.sourceLanguage();
            } catch (Exception e) {

                translatedText = cachedStr;
                sourceLanguage = request.sourceLanguage();
            }

            return new TranslationResponseDomain(translatedText, sourceLanguage, request.targetLanguage(), true);
        }

        log.atInfo().log("Cache miss для translate: key={}, вызов DeepL", cacheKey);
        TranslationResponseDomain response = translator.translate(request);

        try {
            String toCache = objectMapper.writeValueAsString(new TranslationCacheEntry(
                    response.translatedText(), response.sourceLanguage()));
            cache.put(cacheKey, toCache, CacheType.TRANSLATE, translateCacheTtl);
        } catch (Exception e) {
            log.atWarn().log("Не удалось сериализовать TranslationCacheEntry, сохраняем сырой текст: {}", e.getMessage());
            cache.put(cacheKey, response.translatedText(), CacheType.TRANSLATE, translateCacheTtl);
        }

        return response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TranslationCacheEntry(
            @JsonProperty("translated_text") String translatedText,
            @JsonProperty("source_language") String sourceLanguage
    ) {}
}
