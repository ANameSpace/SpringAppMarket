package io.github.anamespace.appservice.dto;

import java.time.Instant;
import java.util.UUID;

public record AppVersionResponse(
        UUID id,
        String versionName,
        String changelog,
        Instant createdAt
) {
}
