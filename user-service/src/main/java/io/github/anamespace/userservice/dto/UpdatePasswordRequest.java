package io.github.anamespace.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "currentPassword must not be blank")
        String currentPassword,
        @NotBlank(message = "newPassword must not be blank")
        @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
        String newPassword
) {
}
