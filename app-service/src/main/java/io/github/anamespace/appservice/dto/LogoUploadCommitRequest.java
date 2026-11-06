package io.github.anamespace.appservice.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoUploadCommitRequest(
        @NotBlank(message = "fileKey обязателен")
        String fileKey
) {
}
