package com.teamfourpixels.deepreaderbackend.controller;

import com.teamfourpixels.deepreaderbackend.dto.AnnotationDto;
import com.teamfourpixels.deepreaderbackend.dto.AnnotationRequest;
import com.teamfourpixels.deepreaderbackend.service.AnnotationService;
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
public class AnnotationController {

    private static final Logger log = LoggerFactory.getLogger(AnnotationController.class);

    private final AnnotationService annotationService;

    public AnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @GetMapping("/api/books/{bookId}/annotations")
    public ResponseEntity<List<AnnotationDto>> getAnnotationsByBook(@PathVariable Long bookId) {
        log.atInfo().log("Получение сносок для книги ID={}", bookId);
        return ResponseEntity.ok(annotationService.findByBookId(bookId));
    }

    @GetMapping("/api/chapters/{chapterId}/annotations")
    public ResponseEntity<List<AnnotationDto>> getAnnotationsByChapter(@PathVariable Long chapterId) {
        log.atInfo().log("Получение сносок для главы ID={}", chapterId);
        return ResponseEntity.ok(annotationService.findByChapterId(chapterId));
    }

    @GetMapping("/api/annotations/{id}")
    public ResponseEntity<AnnotationDto> getAnnotationById(@PathVariable Long id) {
        log.atInfo().log("Получение сноски по ID={}", id);
        return ResponseEntity.ok(annotationService.findById(id));
    }

    @PostMapping("/api/books/{bookId}/annotations")
    public ResponseEntity<AnnotationDto> createAnnotationForBook(
            @PathVariable Long bookId,
            @Valid @RequestBody AnnotationRequest request) {
        log.atInfo().log("Добавление сноски к книге ID={}: «{}»", bookId, request.targetPhrase());
        AnnotationDto created = annotationService.create(bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/annotations/{id}")
    public ResponseEntity<AnnotationDto> updateAnnotation(
            @PathVariable Long id,
            @Valid @RequestBody AnnotationRequest request) {
        log.atInfo().log("Обновление сноски с ID={}", id);
        return ResponseEntity.ok(annotationService.update(id, request));
    }

    @DeleteMapping("/api/annotations/{id}")
    public ResponseEntity<Void> deleteAnnotation(@PathVariable Long id) {
        log.atInfo().log("Удаление сноски с ID={}", id);
        annotationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
