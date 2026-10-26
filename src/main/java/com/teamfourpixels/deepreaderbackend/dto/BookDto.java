package com.teamfourpixels.deepreaderbackend.dto;

import java.time.Instant;

public record BookDto(
        Long id,
        String title,
        String author,
        String genre,
        String language,
        String description,
        String coverUrl,
        Instant createdAt,
        Instant updatedAt,
        int chaptersCount,
        int annotationsCount
) {
    public BookDto(Long id, String title, String author, String genre, String language,
                   String description, String coverUrl, Instant createdAt, Instant updatedAt,
                   Long chaptersCount, Long annotationsCount) {
        this(id, title, author, genre, language, description, coverUrl, createdAt, updatedAt,
                chaptersCount != null ? chaptersCount.intValue() : 0,
                annotationsCount != null ? annotationsCount.intValue() : 0);
    }
}
