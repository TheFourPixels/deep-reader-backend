package com.teamfourpixels.deepreaderbackend.adapter.out.cache;

import com.teamfourpixels.deepreaderbackend.domain.model.CacheType;
import com.teamfourpixels.deepreaderbackend.port.out.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresCacheAdapter implements CachePort {

    private static final Logger log = LoggerFactory.getLogger(PostgresCacheAdapter.class);

    private final JdbcTemplate jdbc;

    public PostgresCacheAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> get(String key) {
        String sql = "SELECT response_data FROM ai_cache_entries WHERE hash_key = ? AND expires_at > NOW()";
        List<String> results = jdbc.queryForList(sql, String.class, key);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public void put(String key, String value, CacheType type, Duration ttl) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttl);
        int updated = jdbc.update("""
                UPDATE ai_cache_entries
                SET response_data = ?, expires_at = ?, cache_type = ?, created_at = now()
                WHERE hash_key = ?
                """, value, expiresAt, type.name(), key);

        if (updated == 0) {
            try {
                jdbc.update("""
                        INSERT INTO ai_cache_entries (hash_key, cache_type, response_data, expires_at, created_at)
                        VALUES (?, ?, ?, ?, now())
                        """, key, type.name(), value, expiresAt);
            } catch (Exception e) {

                jdbc.update("""
                        UPDATE ai_cache_entries
                        SET response_data = ?, expires_at = ?, cache_type = ?, created_at = now()
                        WHERE hash_key = ?
                        """, value, expiresAt, type.name(), key);
            }
        }
        log.atDebug().log("L2 Cache PUT: key={}, type={}, expires={}", key, type, expiresAt);
    }

    @Override
    public void evict(String key) {
        jdbc.update("DELETE FROM ai_cache_entries WHERE hash_key = ?", key);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpired() {
        int deleted = jdbc.update("DELETE FROM ai_cache_entries WHERE expires_at < NOW()");
        if (deleted > 0) {
            log.atInfo().log("L2 Cache cleanup: удалено {} просроченных записей", deleted);
        }
    }

    public void cleanupExpiredEntries() {
        cleanupExpired();
    }
}
