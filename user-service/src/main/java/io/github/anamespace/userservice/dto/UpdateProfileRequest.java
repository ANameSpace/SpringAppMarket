package io.github.anamespace.userservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial profile update. A {@code null} field means "leave unchanged".
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "displayName must be between 1 and 100 characters")
        String displayName,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "avatarColor must be a hex colour like #10B981")
        String avatarColor
) {
}
