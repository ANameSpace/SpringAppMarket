package io.github.anamespace.userservice.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarColor,
        Set<String> roles,
        String status,
        Instant createdAt
) {
}
