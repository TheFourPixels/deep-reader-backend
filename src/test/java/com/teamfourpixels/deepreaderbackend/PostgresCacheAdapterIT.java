package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.adapter.out.cache.PostgresCacheAdapter;
import com.teamfourpixels.deepreaderbackend.domain.model.CacheType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIf("isDockerAvailable")
class PostgresCacheAdapterIT {

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (isDockerAvailable()) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES != null && POSTGRES.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.liquibase.enabled", () -> "true");
        }
    }

    @Autowired
    private PostgresCacheAdapter postgresCacheAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testPutAndGetL2Cache() {

        String hashKey = "a".repeat(64);
        String payload = "{\"test\":\"value\"}";

        postgresCacheAdapter.put(hashKey, payload, CacheType.EXPLAIN, Duration.ofDays(30));

        Optional<String> result = postgresCacheAdapter.get(hashKey);
        assertTrue(result.isPresent());
        assertEquals(payload, result.get());

        String updatedPayload = "{\"test\":\"updated\"}";
        postgresCacheAdapter.put(hashKey, updatedPayload, CacheType.EXPLAIN, Duration.ofDays(30));

        Optional<String> updatedResult = postgresCacheAdapter.get(hashKey);
        assertTrue(updatedResult.isPresent());
        assertEquals(updatedPayload, updatedResult.get());
    }

    @Test
    void testExpiredEntryReturnsEmptyAndIsCleanedUp() {
        String hashKey = "b".repeat(64);
        String payload = "{\"status\":\"expired\"}";

        postgresCacheAdapter.put(hashKey, payload, CacheType.TRANSLATE, Duration.ofSeconds(1));

        jdbcTemplate.update("UPDATE ai_cache_entries SET expires_at = NOW() - INTERVAL '1 hour' WHERE hash_key = ?", hashKey);

        Optional<String> result = postgresCacheAdapter.get(hashKey);
        assertTrue(result.isEmpty(), "Просроченная запись не должна возвращаться");

        postgresCacheAdapter.cleanupExpiredEntries();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_cache_entries WHERE hash_key = ?", Integer.class, hashKey);
        assertNotNull(count);
        assertEquals(0, count, "Просроченная запись должна быть удалена сборщиком мусора");
    }

    @Test
    void testEvict() {
        String hashKey = "c".repeat(64);
        postgresCacheAdapter.put(hashKey, "some_data", CacheType.EXPLAIN, Duration.ofDays(1));

        assertTrue(postgresCacheAdapter.get(hashKey).isPresent());

        postgresCacheAdapter.evict(hashKey);

        assertTrue(postgresCacheAdapter.get(hashKey).isEmpty());
    }
}
