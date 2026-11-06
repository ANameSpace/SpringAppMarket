package io.github.anamespace.appservice.dto;

import java.util.UUID;

public record GalleryImageResponse(
        UUID id,
        String url,
        int sortOrder
) {
}
