package com.teamfourpixels.deepreaderbackend.service;

import com.teamfourpixels.deepreaderbackend.dto.BookDto;
import com.teamfourpixels.deepreaderbackend.dto.BookRequest;
import com.teamfourpixels.deepreaderbackend.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.model.Book;
import com.teamfourpixels.deepreaderbackend.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final DeepReaderMetrics metrics;

    public BookService(BookRepository bookRepository, DeepReaderMetrics metrics) {
        this.bookRepository = bookRepository;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<BookDto> findAll(String query) {
        if (query != null && !query.trim().isEmpty()) {
            metrics.recordBookSearch();
            log.atInfo().log("Поиск книг по поисковому запросу: '{}'", query.trim());
            return bookRepository.searchAsDto(query.trim());
        }
        return bookRepository.findAllAsDto();
    }

    @Transactional(readOnly = true)
    public BookDto findById(Long id) {
        return bookRepository.findDtoById(id)
                .orElseThrow(() -> {
                    log.atError().log("Попытка получения книги: книга с ID {} не найдена", id);
                    return new NoSuchElementException("Книга с ID " + id + " не найдена");
                });
    }

    public BookDto create(BookRequest request) {
        Book book = new Book(
                request.title().trim(),
                request.author().trim(),
                request.genre() != null ? request.genre().trim() : null,
                request.language() != null ? request.language().trim() : "en",
                request.description() != null ? request.description().trim() : null,
                request.coverUrl() != null ? request.coverUrl().trim() : null
        );
        Book saved = bookRepository.save(book);
        metrics.recordBookCreated(saved.getLanguage());
        log.atInfo().log("В библиотеку успешно добавлена новая книга: ID={}, «{}» ({})", saved.getId(), saved.getTitle(), saved.getAuthor());
        return new BookDto(
                saved.getId(),
                saved.getTitle(),
                saved.getAuthor(),
                saved.getGenre(),
                saved.getLanguage(),
                saved.getDescription(),
                saved.getCoverUrl(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                0,
                0
        );
    }

    public BookDto update(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> {
                    log.atError().log("Запрос сущности книги: книга с ID {} не найдена", id);
                    return new NoSuchElementException("Книга с ID " + id + " не найдена");
                });
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        if (request.genre() != null) book.setGenre(request.genre().trim());
        if (request.language() != null) book.setLanguage(request.language().trim());
        if (request.description() != null) book.setDescription(request.description().trim());
        if (request.coverUrl() != null) book.setCoverUrl(request.coverUrl().trim());

        Book updated = bookRepository.saveAndFlush(book);
        log.atInfo().log("Информация о книге успешно обновлена: ID={}, «{}»", updated.getId(), updated.getTitle());
        return bookRepository.findDtoById(updated.getId())
                .orElseThrow(() -> new NoSuchElementException("Книга с ID " + updated.getId() + " не найдена"));
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            log.atError().log("Попытка удаления книги: книга с ID {} не найдена в базе данных", id);
            throw new NoSuchElementException("Книга с ID " + id + " не найдена");
        }
        bookRepository.deleteById(id);
        log.atInfo().log("Книга с ID {} успешно удалена из библиотеки", id);
    }
}
