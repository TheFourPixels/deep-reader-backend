package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record TranslateRequest(

        @JsonProperty("text")
        @NotBlank(message = "text обязателен")
        @Size(min = 1, max = 5000, message = "text должен быть от 1 до 5000 символов")
        String text,

        @JsonProperty("source_language")
        @Size(min = 2, max = 10, message = "source_language должен содержать от 2 до 10 символов")
        String source_language,

        @JsonProperty("target_language")
        @NotBlank(message = "target_language обязателен")
        @Size(min = 2, max = 10, message = "target_language должен содержать от 2 до 10 символов")
        String target_language

) {}
