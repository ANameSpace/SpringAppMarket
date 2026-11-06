package io.github.anamespace.appservice.dto;

import jakarta.validation.constraints.Size;

public record VersionUpdateRequest(
        String versionName,

        @Size(max = 10000, message = "changelog не должен превышать 10000 символов")
        String changelog
) {
}
