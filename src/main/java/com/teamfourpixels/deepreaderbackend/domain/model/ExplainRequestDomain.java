package com.teamfourpixels.deepreaderbackend.domain.model;

public record ExplainRequestDomain(
        String bookId,
        String selectedText,
        String currentSentence,
        SurroundingContext surroundingContext,
        String targetLanguage
) {
    public ExplainRequestDomain {
        if (bookId == null || bookId.isBlank()) throw new IllegalArgumentException("bookId is required");
        if (selectedText == null || selectedText.isBlank()) throw new IllegalArgumentException("selectedText is required");
        if (currentSentence == null || currentSentence.isBlank()) throw new IllegalArgumentException("currentSentence is required");
        if (surroundingContext == null) throw new IllegalArgumentException("surroundingContext is required");
        targetLanguage = (targetLanguage == null || targetLanguage.isBlank()) ? "ru" : targetLanguage;
    }
}
