package com.teamfourpixels.deepreaderbackend.controller;

import com.teamfourpixels.deepreaderbackend.dto.ChapterDto;
import com.teamfourpixels.deepreaderbackend.dto.ChapterRequest;
import com.teamfourpixels.deepreaderbackend.service.ChapterService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChapterController {

    private static final Logger log = LoggerFactory.getLogger(ChapterController.class);

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping("/api/books/{bookId}/chapters")
    public ResponseEntity<List<ChapterDto>> getChaptersByBook(@PathVariable Long bookId) {
        log.atInfo().log("Запрос списка глав для книги ID={}", bookId);
        return ResponseEntity.ok(chapterService.findByBookId(bookId));
    }

    @GetMapping("/api/chapters/{id}")
    public ResponseEntity<ChapterDto> getChapterById(@PathVariable Long id) {
        log.atInfo().log("Получение главы ID={}", id);
        return ResponseEntity.ok(chapterService.findById(id));
    }

    @PostMapping("/api/books/{bookId}/chapters")
    public ResponseEntity<ChapterDto> createChapter(
            @PathVariable Long bookId,
            @Valid @RequestBody ChapterRequest request) {
        log.atInfo().log("Добавление новой главы к книге ID={}: глава №{}, «{}»", bookId, request.chapterNumber(), request.title());
        ChapterDto created = chapterService.create(bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/chapters/{id}")
    public ResponseEntity<ChapterDto> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request) {
        log.atInfo().log("Обновление главы с ID={}", id);
        return ResponseEntity.ok(chapterService.update(id, request));
    }

    @DeleteMapping("/api/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long id) {
        log.atInfo().log("Удаление главы с ID={}", id);
        chapterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
