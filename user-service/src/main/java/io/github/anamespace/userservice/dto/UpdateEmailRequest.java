package io.github.anamespace.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmailRequest(
        @NotBlank(message = "newEmail must not be blank")
        @Email(message = "newEmail must be a valid email address")
        @Size(max = 255, message = "newEmail must be at most 255 characters")
        String newEmail
) {
}
