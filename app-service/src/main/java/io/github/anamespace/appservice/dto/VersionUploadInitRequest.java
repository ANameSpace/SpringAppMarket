package io.github.anamespace.appservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VersionUploadInitRequest(
        @NotBlank(message = "version_name обязателен")
        @Pattern(
                regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
                message = "version_name должен быть SemVer"
        )
        String versionName,

        @Size(max = 10000, message = "changelog не должен превышать 10000 символов")
        String changelog,

        UUID replaceForVersionId
) {
}
