package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ExplainRequest(

        @JsonProperty("book_id")
        @NotBlank(message = "book_id обязателен")
        @Size(min = 1, max = 128, message = "book_id должен быть от 1 до 128 символов")
        String book_id,

        @JsonProperty("selected_text")
        @NotBlank(message = "selected_text обязателен")
        @Size(min = 1, max = 500, message = "selected_text должен быть от 1 до 500 символов")
        String selected_text,

        @JsonProperty("current_sentence")
        @NotBlank(message = "current_sentence обязателен")
        @Size(min = 1, max = 2000, message = "current_sentence должен быть от 1 до 2000 символов")
        String current_sentence,

        @JsonProperty("surrounding_context")
        @NotNull(message = "surrounding_context обязателен")
        @Valid
        SurroundingContextDto surrounding_context,

        @JsonProperty("target_language")
        @Size(min = 2, max = 10, message = "target_language должен содержать от 2 до 10 символов")
        String target_language

) {
    public record SurroundingContextDto(

            @JsonProperty("sentences_before")
            @Size(max = 2, message = "sentences_before: не более 2 предложений")
            List<@Size(max = 1000) String> sentences_before,

            @JsonProperty("sentences_after")
            @Size(max = 2, message = "sentences_after: не более 2 предложений")
            List<@Size(max = 1000) String> sentences_after,

            @JsonProperty("paragraph")
            @Size(max = 4000, message = "paragraph не должен превышать 4000 символов")
            String paragraph,

            @JsonProperty("chapter_context")
            @Size(max = 2000, message = "chapter_context не должен превышать 2000 символов")
            String chapter_context
    ) {}
}
