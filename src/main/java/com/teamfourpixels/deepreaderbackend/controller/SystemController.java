package com.teamfourpixels.deepreaderbackend.controller;

import com.teamfourpixels.deepreaderbackend.dto.StatsDto;
import com.teamfourpixels.deepreaderbackend.repository.AnnotationRepository;
import com.teamfourpixels.deepreaderbackend.repository.BookRepository;
import com.teamfourpixels.deepreaderbackend.repository.ChapterRepository;
import com.teamfourpixels.deepreaderbackend.service.AnnotationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final AnnotationRepository annotationRepository;
    private final AnnotationService annotationService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${PORT:8080}")
    private String serverPort;

    public SystemController(BookRepository bookRepository,
                            ChapterRepository chapterRepository,
                            AnnotationRepository annotationRepository,
                            AnnotationService annotationService,
                            JdbcTemplate jdbcTemplate) {
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.annotationRepository = annotationRepository;
        this.annotationService = annotationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "deep-reader-backend");
        health.put("timestamp", Instant.now().toString());
        health.put("port", serverPort);

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("database", "CONNECTED");
        } catch (Exception e) {
            log.atError()
               .setCause(e)
               .log("Сбой проверки доступности базы данных PostgreSQL: {}", e.getMessage());
            health.put("database", "DISCONNECTED: " + e.getMessage());
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDto> stats() {
        long books = bookRepository.count();
        long chapters = chapterRepository.count();
        long annotations = annotationRepository.count();
        Map<String, Long> byType = annotationService.getCountByType();

        log.atInfo().log("Запрос системной аналитики: книг={}, глав={}, сносок={}", books, chapters, annotations);

        return ResponseEntity.ok(new StatsDto(
                books,
                chapters,
                annotations,
                byType,
                "OK",
                "PostgreSQL"
        ));
    }
}
