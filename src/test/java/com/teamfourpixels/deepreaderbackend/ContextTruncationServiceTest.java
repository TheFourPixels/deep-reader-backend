package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.domain.exception.TextTooLongException;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.SurroundingContext;
import com.teamfourpixels.deepreaderbackend.domain.service.ContextTruncationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContextTruncationServiceTest {

    private ContextTruncationService service;

    @BeforeEach
    void setUp() {

        service = new ContextTruncationService(50, false);
    }

    @Test
    void testCoreUnderBudgetRemainsIntact() {
        SurroundingContext ctx = new SurroundingContext(
                List.of("Sentence before."),
                List.of("Sentence after."),
                "Paragraph content.",
                "Chapter 1."
        );
        ExplainRequestDomain req = new ExplainRequestDomain(
                "book-1",
                "target phrase",
                "This is the current sentence.",
                ctx,
                "ru"
        );

        ExplainRequestDomain result = service.truncate(req);
        assertEquals(req.selectedText(), result.selectedText());
        assertEquals(req.currentSentence(), result.currentSentence());
    }

    @Test
    void testCoreOverBudgetThrowsTextTooLongException() {
        String hugeText = "word ".repeat(100);
        SurroundingContext ctx = new SurroundingContext(List.of(), List.of(), null, null);
        ExplainRequestDomain req = new ExplainRequestDomain(
                "book-1",
                hugeText,
                "Current sentence",
                ctx,
                "ru"
        );

        TextTooLongException ex = assertThrows(TextTooLongException.class, () -> service.truncate(req));
        assertTrue(ex.getMessage().contains("превышает допустимый лимит токенов"));
    }

    @Test
    void testLevel1ChapterContextDroppedWhenTotalExceedsBudget() {
        ContextTruncationService smallBudgetService = new ContextTruncationService(30, false);

        SurroundingContext ctx = new SurroundingContext(
                List.of("Short before."),
                List.of("Short after."),
                null,
                "This is a very long chapter context that should be dropped first to fit within the budget."
        );
        ExplainRequestDomain req = new ExplainRequestDomain(
                "book-1",
                "clocks striking",
                "It was a bright cold day in April.",
                ctx,
                "ru"
        );

        ExplainRequestDomain result = smallBudgetService.truncate(req);
        assertEquals("clocks striking", result.selectedText());
        assertEquals("It was a bright cold day in April.", result.currentSentence());
        assertNull(result.surroundingContext().chapterContext());
    }

    @Test
    void testSystemPromptIsDeductedFromTotalBudgetByDefault() {
        ContextTruncationService prodService = new ContextTruncationService(1000);

        assertTrue(prodService.getMaxContextTokens() < 1000, "System prompt length must be deducted from budget");
        assertTrue(prodService.getMaxContextTokens() > 500, "Remaining context budget should allow adequate literary context");
    }
}
