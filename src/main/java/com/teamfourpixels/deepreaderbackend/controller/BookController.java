package com.teamfourpixels.deepreaderbackend.controller;

import com.teamfourpixels.deepreaderbackend.dto.BookDto;
import com.teamfourpixels.deepreaderbackend.dto.BookRequest;
import com.teamfourpixels.deepreaderbackend.service.BookService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<BookDto>> getAllBooks(@RequestParam(required = false) String search) {
        log.atInfo().log("Получение каталога книг (фильтр поиска: '{}')", search != null ? search : "отсутствует");
        return ResponseEntity.ok(bookService.findAll(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        log.atInfo().log("Получение информации о книге по ID={}", id);
        return ResponseEntity.ok(bookService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookDto> createBook(@Valid @RequestBody BookRequest request) {
        log.atInfo().log("Запрос на добавление новой книги «{}» (автор: {})", request.title(), request.author());
        BookDto created = bookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        log.atInfo().log("Запрос на обновление книги с ID={}", id);
        return ResponseEntity.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        log.atInfo().log("Запрос на удаление книги с ID={}", id);
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
