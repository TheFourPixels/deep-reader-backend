package com.teamfourpixels.deepreaderbackend.domain.model;

public record ExplainResponseDomain(
        String selectedText,
        String contextualMeaning,
        PhenomenonType phenomenonType,
        String explanation,
        StylisticColoring stylisticColoring,
        boolean spoilerAlert,
        boolean cached,
        long processingTimeMs
) {}
