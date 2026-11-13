package com.teamfourpixels.deepreaderbackend.application;

import com.teamfourpixels.deepreaderbackend.domain.exception.EmptyAiResponseException;
import com.teamfourpixels.deepreaderbackend.domain.model.CacheType;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainResponseDomain;
import com.teamfourpixels.deepreaderbackend.domain.service.CacheKeyGenerator;
import com.teamfourpixels.deepreaderbackend.domain.service.ContextTruncationService;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.in.ExplainUseCase;
import com.teamfourpixels.deepreaderbackend.port.out.CachePort;
import com.teamfourpixels.deepreaderbackend.port.out.LlmPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
public class ExplainUseCaseImpl implements ExplainUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExplainUseCaseImpl.class);

    private final ContextTruncationService truncation;
    private final CacheKeyGenerator keyGenerator;
    private final CachePort cache;
    private final LlmPort llm;
    private final ObjectMapper objectMapper;
    private final DeepReaderMetrics metrics;
    private final Duration explainCacheTtl;

    public ExplainUseCaseImpl(ContextTruncationService truncation,
                              CacheKeyGenerator keyGenerator,
                              CachePort cache,
                              LlmPort llm,
                              ObjectMapper objectMapper,
                              @Autowired(required = false) DeepReaderMetrics metrics,
                              @Value("${app.cache.explain-ttl-days:30}") int explainTtlDays) {
        this.truncation = truncation;
        this.keyGenerator = keyGenerator;
        this.cache = cache;
        this.llm = llm;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.explainCacheTtl = Duration.ofDays(explainTtlDays);
    }

    @Override
    public ExplainResponseDomain explain(ExplainRequestDomain request) {
        if (metrics != null) {
            metrics.incrementRequest("explain");
        }
        long start = System.currentTimeMillis();

        ExplainRequestDomain truncated = truncation.truncate(request);

        String cacheKey = keyGenerator.forExplain(truncated);

        Optional<String> cached = cache.get(cacheKey, "explain");
        if (cached.isPresent()) {
            log.atInfo().log("Cache hit для explain: key={}", cacheKey);
            ExplainResponseDomain deserialized = deserialize(cached.get());
            long elapsed = System.currentTimeMillis() - start;
            return withCachedAndTime(deserialized, true, elapsed);
        }

        log.atInfo().log("Cache miss для explain: key={}, вызов LLM", cacheKey);
        ExplainResponseDomain response = llm.explain(truncated);

        if (response.explanation() == null || response.explanation().isBlank()
                || response.contextualMeaning() == null || response.contextualMeaning().isBlank()) {
            throw new EmptyAiResponseException("Провайдер AI вернул пустой ответ. Пожалуйста, повторите запрос.");
        }

        String serialized = serialize(response);
        cache.put(cacheKey, serialized, CacheType.EXPLAIN, explainCacheTtl);

        long elapsed = System.currentTimeMillis() - start;
        return withCachedAndTime(response, false, elapsed);
    }

    private ExplainResponseDomain withCachedAndTime(ExplainResponseDomain r, boolean cached, long ms) {
        return new ExplainResponseDomain(
                r.selectedText(), r.contextualMeaning(), r.phenomenonType(),
                r.explanation(), r.stylisticColoring(), r.spoilerAlert(), cached, ms);
    }

    private String serialize(ExplainResponseDomain response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать ExplainResponse в JSON", e);
        }
    }

    private ExplainResponseDomain deserialize(String json) {
        try {
            return objectMapper.readValue(json, ExplainResponseDomain.class);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось десериализовать ExplainResponse из JSON", e);
        }
    }
}
