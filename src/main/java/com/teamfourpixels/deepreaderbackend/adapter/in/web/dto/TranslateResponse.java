package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TranslateResponse(
        @JsonProperty("translated_text")  String translatedText,
        @JsonProperty("source_language")  String sourceLanguage,
        @JsonProperty("target_language")  String targetLanguage,
        @JsonProperty("cached")           boolean cached
) {}
