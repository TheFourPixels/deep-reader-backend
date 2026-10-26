package com.teamfourpixels.deepreaderbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "Название книги не может быть пустым")
        @Size(max = 255, message = "Название не должно превышать 255 символов")
        String title,

        @NotBlank(message = "Автор не может быть пустым")
        @Size(max = 255, message = "Имя автора не должно превышать 255 символов")
        String author,

        @Size(max = 100, message = "Жанр не должен превышать 100 символов")
        String genre,

        @Size(max = 10, message = "Код языка не должен превышать 10 символов")
        String language,

        String description,

        @Size(max = 512, message = "Ссылка на обложку не должна превышать 512 символов")
        String coverUrl
) {}
