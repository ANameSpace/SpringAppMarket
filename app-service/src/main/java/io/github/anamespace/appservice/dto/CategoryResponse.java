package io.github.anamespace.appservice.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String slug,
        String name,
        String svgIcon,
        Instant createdAt
) {
}
