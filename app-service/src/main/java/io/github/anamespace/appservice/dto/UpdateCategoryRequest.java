package io.github.anamespace.appservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Pattern(regexp = "^[a-z0-9-]{2,64}$", message = "slug должен соответствовать ^[a-z0-9-]{2,64}$")
        String slug,

        @Size(min = 1, max = 100, message = "name должен содержать от 1 до 100 символов")
        String name,

        String svgIcon
) {
}
