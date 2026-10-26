package com.teamfourpixels.deepreaderbackend.service;

import com.teamfourpixels.deepreaderbackend.dto.AnnotationDto;
import com.teamfourpixels.deepreaderbackend.dto.AnnotationRequest;
import com.teamfourpixels.deepreaderbackend.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.model.Annotation;
import com.teamfourpixels.deepreaderbackend.model.AnnotationType;
import com.teamfourpixels.deepreaderbackend.model.Book;
import com.teamfourpixels.deepreaderbackend.model.Chapter;
import com.teamfourpixels.deepreaderbackend.repository.AnnotationRepository;
import com.teamfourpixels.deepreaderbackend.repository.BookRepository;
import com.teamfourpixels.deepreaderbackend.repository.ChapterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@Transactional
public class AnnotationService {

    private static final Logger log = LoggerFactory.getLogger(AnnotationService.class);

    private final AnnotationRepository annotationRepository;
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final DeepReaderMetrics metrics;

    public AnnotationService(AnnotationRepository annotationRepository,
                             BookRepository bookRepository,
                             ChapterRepository chapterRepository,
                             DeepReaderMetrics metrics) {
        this.annotationRepository = annotationRepository;
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<AnnotationDto> findAll() {
        return annotationRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnotationDto> findByBookId(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            log.atError().log("Попытка получения сносок: книга с ID {} не найдена", bookId);
            throw new NoSuchElementException("Книга с ID " + bookId + " не найдена");
        }
        return annotationRepository.findByBookId(bookId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnotationDto> findByChapterId(Long chapterId) {
        if (!chapterRepository.existsById(chapterId)) {
            log.atError().log("Попытка получения сносок: глава с ID {} не найдена", chapterId);
            throw new NoSuchElementException("Глава с ID " + chapterId + " не найдена");
        }
        return annotationRepository.findByChapterId(chapterId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AnnotationDto findById(Long id) {
        Annotation annotation = annotationRepository.findById(id)
                .orElseThrow(() -> {
                    log.atError().log("Попытка получения сноски: сноска с ID {} не найдена", id);
                    return new NoSuchElementException("Аннотация с ID " + id + " не найдена");
                });
        return toDto(annotation);
    }

    public AnnotationDto create(Long bookId, AnnotationRequest request) {
        long startNanos = System.nanoTime();
        if (!bookRepository.existsById(bookId)) {
            log.atError().log("Попытка создания сноски: книга с ID {} не найдена", bookId);
            throw new NoSuchElementException("Книга с ID " + bookId + " не найдена");
        }
        Book bookProxy = bookRepository.getReferenceById(bookId);

        Chapter chapterProxy = null;
        if (request.chapterId() != null) {
            if (!chapterRepository.existsById(request.chapterId())) {
                log.atError().log("Попытка создания сноски: глава с ID {} не найдена", request.chapterId());
                throw new NoSuchElementException("Глава с ID " + request.chapterId() + " не найдена");
            }
            chapterProxy = chapterRepository.getReferenceById(request.chapterId());
        }

        Annotation annotation = new Annotation(
                bookProxy,
                chapterProxy,
                request.targetPhrase().trim(),
                request.explanation().trim(),
                request.type(),
                request.contextSnippet() != null ? request.contextSnippet().trim() : null
        );

        Annotation saved = annotationRepository.save(annotation);
        metrics.recordAnnotationCreated(saved.getType().name(), saved.getChapter() != null);
        metrics.recordAnnotationCreationDuration(Duration.ofNanos(System.nanoTime() - startNanos));

        log.atInfo().log("Создана новая контекстная сноска ID={}: фраза='{}', тип={}, книга ID={}",
                saved.getId(), saved.getTargetPhrase(), saved.getType(), bookId);
        return toDto(saved);
    }

    public AnnotationDto update(Long id, AnnotationRequest request) {
        Annotation annotation = annotationRepository.findById(id)
                .orElseThrow(() -> {
                    log.atError().log("Попытка обновления сноски: сноска с ID {} не найдена", id);
                    return new NoSuchElementException("Аннотация с ID " + id + " не найдена");
                });

        if (request.chapterId() != null) {
            if (!chapterRepository.existsById(request.chapterId())) {
                log.atError().log("Попытка обновления сноски: целевая глава с ID {} не найдена", request.chapterId());
                throw new NoSuchElementException("Глава с ID " + request.chapterId() + " не найдена");
            }
            Chapter newChapterProxy = chapterRepository.getReferenceById(request.chapterId());
            annotation.setChapter(newChapterProxy);
        } else {
            annotation.setChapter(null);
        }

        annotation.setTargetPhrase(request.targetPhrase().trim());
        annotation.setExplanation(request.explanation().trim());
        annotation.setType(request.type());
        annotation.setContextSnippet(request.contextSnippet() != null ? request.contextSnippet().trim() : null);

        Annotation updated = annotationRepository.save(annotation);
        log.atInfo().log("Сноска ID={} успешно обновлена: фраза='{}', категория={}", updated.getId(), updated.getTargetPhrase(), updated.getType());
        return toDto(updated);
    }

    public void delete(Long id) {
        if (!annotationRepository.existsById(id)) {
            log.atError().log("Попытка удаления сноски: сноска с ID {} не найдена в базе данных", id);
            throw new NoSuchElementException("Аннотация с ID " + id + " не найдена");
        }
        annotationRepository.deleteById(id);
        log.atInfo().log("Сноска с ID={} успешно удалена", id);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getCountByType() {
        Map<String, Long> counts = new HashMap<>();
        for (AnnotationType type : AnnotationType.values()) {
            counts.put(type.name(), annotationRepository.countByType(type));
        }
        return counts;
    }

    public AnnotationDto toDto(Annotation annotation) {
        return new AnnotationDto(
                annotation.getId(),
                annotation.getBook().getId(),
                annotation.getChapter() != null ? annotation.getChapter().getId() : null,
                annotation.getTargetPhrase(),
                annotation.getExplanation(),
                annotation.getType(),
                annotation.getType().getDisplayName(),
                annotation.getContextSnippet(),
                annotation.getCreatedAt()
        );
    }
}
