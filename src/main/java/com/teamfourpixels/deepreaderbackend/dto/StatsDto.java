package com.teamfourpixels.deepreaderbackend.dto;

import java.util.Map;

public record StatsDto(
        long totalBooks,
        long totalChapters,
        long totalAnnotations,
        Map<String, Long> annotationsByType,
        String status,
        String database
) {}
