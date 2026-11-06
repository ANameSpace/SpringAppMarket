package io.github.anamespace.appservice.dto;

import java.time.Instant;

public record UploadInitResponse(
        String uploadUrl,
        String fileKey,
        Instant expiresAt
) {
}
