package com.teamfourpixels.deepreaderbackend.dto;

import com.teamfourpixels.deepreaderbackend.model.AnnotationType;
import java.time.Instant;

public record AnnotationDto(
        Long id,
        Long bookId,
        Long chapterId,
        String targetPhrase,
        String explanation,
        AnnotationType type,
        String typeDisplayName,
        String contextSnippet,
        Instant createdAt
) {}
