package com.teamfourpixels.deepreaderbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
        @NotNull(message = "Номер главы обязателен")
        @Positive(message = "Номер главы должен быть положительным числом")
        Integer chapterNumber,

        @NotBlank(message = "Заголовок главы не может быть пустым")
        @Size(max = 255, message = "Заголовок главы не должен превышать 255 символов")
        String title,

        @NotBlank(message = "Содержимое главы не может быть пустым")
        @Size(max = 1000000, message = "Содержимое главы не должно превышать 1 000 000 символов")
        String content
) {}
