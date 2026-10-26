package com.teamfourpixels.deepreaderbackend.service;

import com.teamfourpixels.deepreaderbackend.model.Annotation;
import com.teamfourpixels.deepreaderbackend.model.AnnotationType;
import com.teamfourpixels.deepreaderbackend.model.Book;
import com.teamfourpixels.deepreaderbackend.model.Chapter;
import com.teamfourpixels.deepreaderbackend.repository.AnnotationRepository;
import com.teamfourpixels.deepreaderbackend.repository.BookRepository;
import com.teamfourpixels.deepreaderbackend.repository.ChapterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializerService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerService.class);

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final AnnotationRepository annotationRepository;

    public DataInitializerService(BookRepository bookRepository,
                                  ChapterRepository chapterRepository,
                                  AnnotationRepository annotationRepository) {
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.annotationRepository = annotationRepository;
    }

    @Override
    public void run(String... args) {
        long booksCount = bookRepository.count();
        if (booksCount > 0) {
            log.atInfo().log("База данных уже содержит произведения (всего: {}), первичная инициализация пропущена.", booksCount);
            return;
        }

        log.atInfo().log("Инициализация демонстрационного произведения «Великий Гэтсби» и базы сносок для Deep Reader...");

        Book gatsby = new Book(
                "The Great Gatsby",
                "F. Scott Fitzgerald",
                "Classic Literature",
                "en",
                "Роман о богатстве, иллюзиях и трагедии эпохи джаза. Идеальный текст для анализа скрытого подтекста, сленга 1920-х годов и глубоких авторских метафор.",
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&q=80"
        );
        Book savedBook = bookRepository.save(gatsby);

        String chapterContent = 
                "In my younger and more vulnerable years my father gave me some advice that I've been turning over in my mind ever since. " +
                "\"Whenever you feel like criticizing anyone,\" he told me, \"just remember that all the people in this world haven't had the advantages that you've had.\"\n\n" +
                "He didn't say any more, but we've always been unusually communicative in a reserved way, and I understood that he meant a great deal more than that. " +
                "In consequence, I'm inclined to reserve all judgements, a habit that has opened up many curious natures to me and also made me the victim of not a few veteran bores.\n\n" +
                "When I came back from the East last autumn I felt that I wanted the world to be in uniform and at a sort of moral attention forever. " +
                "Only Gatsby, the man who gives his name to this book, was exempt from my reaction—Gatsby, who represented everything for which I have an unaffected scorn.\n\n" +
                "He smiled understandingly—much more than understandingly. It was one of those rare smiles with a quality of eternal reassurance in it. " +
                "\"Don't mention it, old sport,\" he whispered with an easy nonchalance, raising his champagne glass. " +
                "He looked across the bay where the single green light faintly pulsed through the mist at the dock, a phantom promise in the dark.";

        Chapter chapter1 = new Chapter(savedBook, 1, "Chapter I: The Green Light and Moral Scorn", chapterContent);
        Chapter savedChapter = chapterRepository.save(chapter1);

        Annotation a1 = new Annotation(
                savedBook,
                savedChapter,
                "old sport",
                "Фирменное обращение Гэтсби (британский аристократический сленг конца XIX — начала XX века). Гэтсби использует его нарочито, пытаясь сымитировать манеры выпускника Оксфорда и скрыть свое плебейское происхождение.",
                AnnotationType.SLANG,
                "\"Don't mention it, old sport,\" he whispered with an easy nonchalance..."
        );

        Annotation a2 = new Annotation(
                savedBook,
                savedChapter,
                "moral attention",
                "Военная метафора. Ник Каррауэй сравнивает свое душевное состояние после возвращения с войны и Нью-Йорка с шеренгой солдат, вставших «смирно». Он жаждет строгой моральной дисциплины взамен распущенности высшего общества.",
                AnnotationType.METAPHOR,
                "I felt that I wanted the world to be in uniform and at a sort of moral attention forever."
        );

        Annotation a3 = new Annotation(
                savedBook,
                savedChapter,
                "green light",
                "Центральный аллегорический образ американской мечты и недосягаемости прошлого. Зеленый огонек на причале Дэйзи — символ тоски, идеализации и погони за недостижимым идеалом.",
                AnnotationType.ALLUSION,
                "He looked across the bay where the single green light faintly pulsed through the mist at the dock..."
        );

        Annotation a4 = new Annotation(
                savedBook,
                savedChapter,
                "veteran bores",
                "Ироничная идиома, означающая закоренелых, профессиональных зануд, чьи бесконечные исповеди Ник был вынужден выслушивать из-за своей привычки не осуждать людей сразу.",
                AnnotationType.IDIOM,
                "...a habit that has opened up many curious natures to me and also made me the victim of not a few veteran bores."
        );

        annotationRepository.save(a1);
        annotationRepository.save(a2);
        annotationRepository.save(a3);
        annotationRepository.save(a4);

        log.atInfo().log("Демонстрационные данные успешно инициализированы: книга «{}», 1 глава и {} сносок.", savedBook.getTitle(), 4);
    }
}
