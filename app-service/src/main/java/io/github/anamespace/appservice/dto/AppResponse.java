package io.github.anamespace.appservice.dto;

import io.github.anamespace.appservice.entity.DownloadState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppResponse(
        UUID id,
        String slug,
        String name,
        String description,
        Long developerId,
        BigDecimal price,
        List<CategoryResponse> categories,
        List<String> tags,
        String logoUrl,
        List<String> galleryUrls,
        AppVersionResponse latestVersion,
        BigDecimal cachedRating,
        int cachedRatingCount,
        long downloadCount,
        String status,
        DownloadState downloadState,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
