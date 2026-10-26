package com.teamfourpixels.deepreaderbackend.service;

import com.teamfourpixels.deepreaderbackend.dto.ChapterDto;
import com.teamfourpixels.deepreaderbackend.dto.ChapterRequest;
import com.teamfourpixels.deepreaderbackend.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.model.Book;
import com.teamfourpixels.deepreaderbackend.model.Chapter;
import com.teamfourpixels.deepreaderbackend.repository.AnnotationRepository;
import com.teamfourpixels.deepreaderbackend.repository.BookRepository;
import com.teamfourpixels.deepreaderbackend.repository.ChapterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ChapterService {

    private static final Logger log = LoggerFactory.getLogger(ChapterService.class);

    private final ChapterRepository chapterRepository;
    private final BookRepository bookRepository;
    private final AnnotationRepository annotationRepository;
    private final DeepReaderMetrics metrics;

    public ChapterService(ChapterRepository chapterRepository,
                          BookRepository bookRepository,
                          AnnotationRepository annotationRepository,
                          DeepReaderMetrics metrics) {
        this.chapterRepository = chapterRepository;
        this.bookRepository = bookRepository;
        this.annotationRepository = annotationRepository;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<ChapterDto> findByBookId(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            log.atError().log("Попытка получения глав: книга с ID {} не найдена", bookId);
            throw new NoSuchElementException("Книга с ID " + bookId + " не найдена");
        }
        return chapterRepository.findByBookIdOrderByChapterNumberAsc(bookId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChapterDto findById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> {
                    log.atError().log("Попытка получения главы: глава с ID {} не найдена", id);
                    return new NoSuchElementException("Глава с ID " + id + " не найдена");
                });
        metrics.recordChapterRead(chapter.getBook().getId());
        return toDto(chapter);
    }

    public ChapterDto create(Long bookId, ChapterRequest request) {
        if (!bookRepository.existsById(bookId)) {
            log.atError().log("Попытка создания главы: книга с ID {} не найдена", bookId);
            throw new NoSuchElementException("Книга с ID " + bookId + " не найдена");
        }
        Book bookProxy = bookRepository.getReferenceById(bookId);
        Chapter chapter = new Chapter(
                bookProxy,
                request.chapterNumber(),
                request.title().trim(),
                request.content()
        );
        bookProxy.addChapter(chapter);
        Chapter saved = chapterRepository.save(chapter);
        log.atInfo().log("К книге ID={} успешно добавлена глава №{}: «{}» (ID={})", bookId, saved.getChapterNumber(), saved.getTitle(), saved.getId());
        return toDto(saved);
    }

    public ChapterDto update(Long id, ChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> {
                    log.atError().log("Запрос сущности главы: глава с ID {} не найдена", id);
                    return new NoSuchElementException("Глава с ID " + id + " не найдена");
                });
        chapter.setChapterNumber(request.chapterNumber());
        chapter.setTitle(request.title().trim());
        chapter.setContent(request.content());
        Chapter updated = chapterRepository.save(chapter);
        log.atInfo().log("Глава с ID={} успешно обновлена: №{}, «{}»", id, updated.getChapterNumber(), updated.getTitle());
        return toDto(updated);
    }

    public void delete(Long id) {
        if (!chapterRepository.existsById(id)) {
            log.atError().log("Попытка удаления главы: глава с ID {} не найдена в базе данных", id);
            throw new NoSuchElementException("Глава с ID " + id + " не найдена");
        }
        chapterRepository.deleteById(id);
        log.atInfo().log("Глава с ID={} успешно удалена", id);
    }

    public ChapterDto toDto(Chapter chapter) {
        int annotationsCount = (int) annotationRepository.countByChapterId(chapter.getId());
        return new ChapterDto(
                chapter.getId(),
                chapter.getBook().getId(),
                chapter.getChapterNumber(),
                chapter.getTitle(),
                chapter.getContent(),
                chapter.getCreatedAt(),
                annotationsCount
        );
    }
}
