package com.teamfourpixels.deepreaderbackend.metrics;

import com.teamfourpixels.deepreaderbackend.repository.AnnotationRepository;
import com.teamfourpixels.deepreaderbackend.repository.BookRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DeepReaderMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer annotationCreateTimer;

    public DeepReaderMetrics(MeterRegistry meterRegistry,
                             BookRepository bookRepository,
                             AnnotationRepository annotationRepository) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("deepreader.active.books.count", bookRepository, BookRepository::count)
                .description("Текущее общее количество доступных книг в библиотеке Deep Reader")
                .register(meterRegistry);

        Gauge.builder("deepreader.active.annotations.count", annotationRepository, AnnotationRepository::count)
                .description("Текущее общее количество литературоведческих аннотаций в базе данных")
                .register(meterRegistry);

        this.annotationCreateTimer = Timer.builder("deepreader.annotation.create.duration")
                .description("Время создания и сохранения новой литературоведческой сноски")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    public void recordBookCreated(String language) {
        Counter.builder("deepreader.books.created.total")
                .description("Общее число добавленных в библиотеку книг")
                .tag("language", language != null && !language.isBlank() ? language.toLowerCase() : "unknown")
                .register(meterRegistry)
                .increment();
    }

    public void recordAnnotationCreated(String type, boolean hasChapter) {
        Counter.builder("deepreader.annotations.created.total")
                .description("Общее число созданных литературоведческих сносок")
                .tag("type", type != null ? type : "UNKNOWN")
                .tag("has_chapter", Boolean.toString(hasChapter))
                .register(meterRegistry)
                .increment();
    }

    public void recordChapterRead(Long bookId) {
        Counter.builder("deepreader.chapters.read.total")
                .description("Число прочтений/запросов глав произведений читателями")
                .tag("book_id", bookId != null ? bookId.toString() : "unknown")
                .register(meterRegistry)
                .increment();
    }

    public void recordBookSearch() {
        Counter.builder("deepreader.book.searches.total")
                .description("Число выполненных читателями поисковых запросов по книгам")
                .register(meterRegistry)
                .increment();
    }

    public void recordAnnotationCreationDuration(Duration duration) {
        annotationCreateTimer.record(duration);
    }
}
