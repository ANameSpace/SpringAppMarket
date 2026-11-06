package io.github.anamespace.appservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "slug обязателен")
        @Pattern(regexp = "^[a-z0-9-]{2,64}$", message = "slug должен соответствовать ^[a-z0-9-]{2,64}$")
        String slug,

        @NotBlank(message = "name обязателен")
        @Size(max = 100, message = "name не должен превышать 100 символов")
        String name,

        String svgIcon
) {
}
