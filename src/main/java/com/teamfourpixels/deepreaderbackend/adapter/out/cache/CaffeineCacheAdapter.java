package com.teamfourpixels.deepreaderbackend.adapter.out.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.teamfourpixels.deepreaderbackend.domain.model.CacheType;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.out.CachePort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Primary
@Component
public class CaffeineCacheAdapter implements CachePort {

    private static final Logger log = LoggerFactory.getLogger(CaffeineCacheAdapter.class);

    private final PostgresCacheAdapter l2;
    private final DeepReaderMetrics metrics;
    private final int maxSize;
    private final int ttlHours;
    private Cache<String, String> l1;

    public CaffeineCacheAdapter(PostgresCacheAdapter l2,
                                DeepReaderMetrics metrics,
                                @Value("${app.cache.l1-max-size:10000}") int maxSize,
                                @Value("${app.cache.l1-ttl-hours:1}") int ttlHours) {
        this.l2 = l2;
        this.metrics = metrics;
        this.maxSize = maxSize;
        this.ttlHours = ttlHours;
    }

    @PostConstruct
    void init() {
        this.l1 = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofHours(ttlHours))
                .build();
        log.atInfo().log("L1 Caffeine cache инициализирован: maxSize={}, ttlHours={}", maxSize, ttlHours);
    }

    @Override
    public Optional<String> get(String key) {
        return get(key, "explain");
    }

    @Override
    public Optional<String> get(String key, String action) {
        String l1Hit = l1.getIfPresent(key);
        if (l1Hit != null) {
            metrics.recordCacheRequest(action, "L1", "hit");
            log.atDebug().log("L1 Cache HIT: action={}, key={}", action, key);
            return Optional.of(l1Hit);
        }
        metrics.recordCacheRequest(action, "L1", "miss");

        Optional<String> l2Hit = l2.get(key);
        if (l2Hit.isPresent()) {
            metrics.recordCacheRequest(action, "L2", "hit");
            log.atDebug().log("L2 Cache HIT (promo to L1): action={}, key={}", action, key);
            l1.put(key, l2Hit.get());
            return l2Hit;
        } else {
            metrics.recordCacheRequest(action, "L2", "miss");
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, String value, CacheType type, Duration ttl) {
        l1.put(key, value);
        l2.put(key, value, type, ttl);
    }

    @Override
    public void evict(String key) {
        l1.invalidate(key);
        l2.evict(key);
    }
}
