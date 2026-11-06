package io.github.anamespace.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username must not be blank")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
                message = "username may contain letters, digits, dots, dashes and underscores")
        String username,
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password
) {
}
