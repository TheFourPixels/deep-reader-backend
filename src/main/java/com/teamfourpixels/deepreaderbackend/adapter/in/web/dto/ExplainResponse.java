package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExplainResponse(
        @JsonProperty("selected_text")      String selectedText,
        @JsonProperty("contextual_meaning") String contextualMeaning,
        @JsonProperty("phenomenon_type")    String phenomenonType,
        @JsonProperty("explanation")        String explanation,
        @JsonProperty("stylistic_coloring") String stylisticColoring,
        @JsonProperty("spoiler_alert")      boolean spoilerAlert,
        @JsonProperty("cached")             boolean cached,
        @JsonProperty("processing_time_ms") long processingTimeMs
) {}
