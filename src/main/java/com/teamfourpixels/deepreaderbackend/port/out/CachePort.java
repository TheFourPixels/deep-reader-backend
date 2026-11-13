package com.teamfourpixels.deepreaderbackend.port.out;

import com.teamfourpixels.deepreaderbackend.domain.model.CacheType;

import java.time.Duration;
import java.util.Optional;

public interface CachePort {

    Optional<String> get(String key);

    default Optional<String> get(String key, String action) {
        return get(key);
    }

    void put(String key, String value, CacheType type, Duration ttl);

    void evict(String key);
}
