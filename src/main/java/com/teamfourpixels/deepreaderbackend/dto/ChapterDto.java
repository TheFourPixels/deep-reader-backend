package com.teamfourpixels.deepreaderbackend.dto;

import java.time.Instant;

public record ChapterDto(
        Long id,
        Long bookId,
        Integer chapterNumber,
        String title,
        String content,
        Instant createdAt,
        int annotationsCount
) {}
