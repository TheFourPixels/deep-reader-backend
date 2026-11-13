package com.teamfourpixels.deepreaderbackend.adapter.in.web;

import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final JdbcTemplate jdbcTemplate;
    private final String activeLlmProvider;

    public SystemController(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Value("${app.ai.llm.provider:openai}") String llmProvider,
            @Value("${app.ai.openai.model:gpt-4o-mini}") String openAiModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.activeLlmProvider = "mock".equalsIgnoreCase(llmProvider)
                ? "MockLlmAdapter"
                : ("openai".equalsIgnoreCase(llmProvider)
                        ? "OpenAiAdapter (" + openAiModel + ")"
                        : "Unknown (" + llmProvider + ")");
    }

    @GetMapping({"/api/v1/health", "/api/health"})
    public ResponseEntity<HealthResponse> health() {
        String cacheStatus;
        boolean isUp = true;

        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("SELECT 1 FROM ai_cache_entries LIMIT 1");
                cacheStatus = "CONNECTED";
            } catch (Exception e) {
                log.atError()
                   .setCause(e)
                   .log("Проверка доступности L2 кэша PostgreSQL завершилась с ошибкой: {}", e.getMessage());
                cacheStatus = "DISCONNECTED: " + e.getMessage();
                isUp = false;
            }
        } else {
            cacheStatus = "IN_MEMORY_ONLY";
        }

        String overallStatus = isUp ? "UP" : "DOWN";
        HealthResponse response = new HealthResponse(
                overallStatus,
                "deep-reader-backend",
                Instant.now().toString(),
                cacheStatus,
                activeLlmProvider
        );

        return isUp
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
