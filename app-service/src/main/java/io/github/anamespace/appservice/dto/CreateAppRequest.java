package io.github.anamespace.appservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateAppRequest(
        @NotBlank(message = "name обязателен")
        @Size(max = 200, message = "name не должен превышать 200 символов")
        String name,

        @NotBlank(message = "slug обязателен")
        @Pattern(regexp = "^[a-zA-Z0-9]{2,64}$", message = "slug должен соответствовать ^[a-zA-Z0-9]{2,64}$")
        String slug,

        @Size(max = 5000, message = "description не должен превышать 5000 символов")
        String description,

        @NotNull(message = "price обязателен")
        @DecimalMin(value = "0.00", message = "price должен быть неотрицательным")
        @Digits(integer = 8, fraction = 2, message = "price должен иметь не более 8 цифр до и 2 после запятой")
        BigDecimal price,

        @Size(max = 3, message = "categoryIds не должен содержать более 3 элементов")
        List<UUID> categoryIds
) {
}
