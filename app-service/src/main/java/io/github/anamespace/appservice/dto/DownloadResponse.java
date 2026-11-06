package io.github.anamespace.appservice.dto;

import java.time.Instant;

public record DownloadResponse(
        String downloadUrl,
        String fileName,
        Instant expiresAt
) {
}
