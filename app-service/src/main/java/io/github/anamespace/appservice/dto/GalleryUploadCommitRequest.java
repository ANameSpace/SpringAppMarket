package io.github.anamespace.appservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GalleryUploadCommitRequest(
        @NotBlank(message = "fileKey обязателен")
        String fileKey,

        @Min(value = 0, message = "sortOrder должен быть от 0 до 9")
        @Max(value = 9, message = "sortOrder должен быть от 0 до 9")
        int sortOrder
) {
}
