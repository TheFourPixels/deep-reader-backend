package com.teamfourpixels.deepreaderbackend.dto;

import com.teamfourpixels.deepreaderbackend.model.AnnotationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnnotationRequest(
        Long chapterId,

        @NotBlank(message = "Целевая фраза / сленг / метафора не может быть пустой")
        @Size(max = 512, message = "Длина целевой фразы не должна превышать 512 символов")
        String targetPhrase,

        @NotBlank(message = "Объяснение не может быть пустым")
        String explanation,

        @NotNull(message = "Тип подсказки обязателен")
        AnnotationType type,

        String contextSnippet
) {}
