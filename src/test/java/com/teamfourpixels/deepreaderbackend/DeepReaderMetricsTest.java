package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.repository.AnnotationRepository;
import com.teamfourpixels.deepreaderbackend.repository.BookRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DeepReaderMetricsTest {

    private MeterRegistry meterRegistry;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AnnotationRepository annotationRepository;

    private DeepReaderMetrics deepReaderMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        lenient().when(bookRepository.count()).thenReturn(15L);
        lenient().when(annotationRepository.count()).thenReturn(42L);

        deepReaderMetrics = new DeepReaderMetrics(meterRegistry, bookRepository, annotationRepository);
    }

    @Test
    @DisplayName("Gauges должны корректно возвращать текущее количество книг и аннотаций")
    void testGaugesReflectCounts() {
        Gauge booksGauge = meterRegistry.find("deepreader.active.books.count").gauge();
        assertThat(booksGauge).isNotNull();
        assertThat(booksGauge.value()).isEqualTo(15.0);

        Gauge annotationsGauge = meterRegistry.find("deepreader.active.annotations.count").gauge();
        assertThat(annotationsGauge).isNotNull();
        assertThat(annotationsGauge.value()).isEqualTo(42.0);
    }

    @Test
    @DisplayName("Счётчик deepreader.books.created.total должен инкрементироваться с тегом языка")
    void testBookCreatedCounter() {
        deepReaderMetrics.recordBookCreated("ru");
        deepReaderMetrics.recordBookCreated("ru");
        deepReaderMetrics.recordBookCreated("en");

        Counter ruCounter = meterRegistry.find("deepreader.books.created.total")
                .tag("language", "ru")
                .counter();
        assertThat(ruCounter).isNotNull();
        assertThat(ruCounter.count()).isEqualTo(2.0);

        Counter enCounter = meterRegistry.find("deepreader.books.created.total")
                .tag("language", "en")
                .counter();
        assertThat(enCounter).isNotNull();
        assertThat(enCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Счётчик deepreader.annotations.created.total должен фиксировать типы и привязку к главам")
    void testAnnotationCreatedCounter() {
        deepReaderMetrics.recordAnnotationCreated("LITERARY_ALLUSION", true);
        deepReaderMetrics.recordAnnotationCreated("LITERARY_ALLUSION", false);

        Counter counterWithChapter = meterRegistry.find("deepreader.annotations.created.total")
                .tag("type", "LITERARY_ALLUSION")
                .tag("has_chapter", "true")
                .counter();
        assertThat(counterWithChapter).isNotNull();
        assertThat(counterWithChapter.count()).isEqualTo(1.0);

        Counter counterWithoutChapter = meterRegistry.find("deepreader.annotations.created.total")
                .tag("type", "LITERARY_ALLUSION")
                .tag("has_chapter", "false")
                .counter();
        assertThat(counterWithoutChapter).isNotNull();
        assertThat(counterWithoutChapter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Счётчик deepreader.chapters.read.total должен учитывать прочтения глав")
    void testChaptersReadCounter() {
        deepReaderMetrics.recordChapterRead(10L);
        deepReaderMetrics.recordChapterRead(10L);

        Counter counter = meterRegistry.find("deepreader.chapters.read.total")
                .tag("book_id", "10")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Таймер deepreader.annotation.create.duration должен фиксировать время создания сносок")
    void testAnnotationTimer() {
        deepReaderMetrics.recordAnnotationCreationDuration(Duration.ofMillis(120));

        var timer = meterRegistry.find("deepreader.annotation.create.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(120.0);
    }
}
