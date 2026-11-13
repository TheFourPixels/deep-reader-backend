package com.teamfourpixels.deepreaderbackend.domain.model;

public record TranslationRequestDomain(
        String text,
        String sourceLanguage,
        String targetLanguage
) {
    public TranslationRequestDomain {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text is required");
        if (targetLanguage == null || targetLanguage.isBlank()) throw new IllegalArgumentException("targetLanguage is required");
    }
}
