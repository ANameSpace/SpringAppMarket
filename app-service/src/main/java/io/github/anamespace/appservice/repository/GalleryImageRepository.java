package io.github.anamespace.appservice.repository;

import io.github.anamespace.appservice.entity.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, UUID> {

    List<GalleryImage> findByAppIdOrderBySortOrderAsc(UUID appId);

    Optional<GalleryImage> findByAppIdAndId(UUID appId, UUID id);

    long countByAppId(UUID appId);

    boolean existsByAppIdAndSortOrder(UUID appId, int sortOrder);

    void deleteByAppId(UUID appId);
}
