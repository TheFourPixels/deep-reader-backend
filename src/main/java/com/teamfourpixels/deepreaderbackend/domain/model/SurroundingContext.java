package com.teamfourpixels.deepreaderbackend.domain.model;

import java.util.ArrayList;
import java.util.List;

public record SurroundingContext(
        List<String> sentencesBefore,
        List<String> sentencesAfter,
        String paragraph,
        String chapterContext
) {
    public SurroundingContext {
        sentencesBefore = sentencesBefore != null ? List.copyOf(sentencesBefore) : List.of();
        sentencesAfter  = sentencesAfter  != null ? List.copyOf(sentencesAfter)  : List.of();
    }
}
