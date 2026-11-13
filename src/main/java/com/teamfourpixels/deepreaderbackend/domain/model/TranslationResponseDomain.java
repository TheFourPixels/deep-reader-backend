package com.teamfourpixels.deepreaderbackend.domain.model;

public record TranslationResponseDomain(
        String translatedText,
        String sourceLanguage,
        String targetLanguage,
        boolean cached
) {}
