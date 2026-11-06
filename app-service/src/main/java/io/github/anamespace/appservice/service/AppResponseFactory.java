package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.config.AppProperties;
import io.github.anamespace.appservice.dto.AppResponse;
import io.github.anamespace.appservice.dto.AppVersionResponse;
import io.github.anamespace.appservice.dto.CategoryResponse;
import io.github.anamespace.appservice.dto.GalleryImageResponse;
import io.github.anamespace.appservice.entity.App;
import io.github.anamespace.appservice.entity.AppStatus;
import io.github.anamespace.appservice.entity.AppVersion;
import io.github.anamespace.appservice.entity.Category;
import io.github.anamespace.appservice.entity.GalleryImage;
import io.github.anamespace.appservice.repository.AppVersionRepository;
import io.github.anamespace.appservice.repository.GalleryImageRepository;
import io.github.anamespace.appservice.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class AppResponseFactory {

    private final AppVersionRepository versionRepository;
    private final GalleryImageRepository galleryRepository;
    private final StorageService storageService;
    private final DownloadStateService downloadStateService;
    private final AppProperties properties;

    public AppResponseFactory(AppVersionRepository versionRepository,
                              GalleryImageRepository galleryRepository,
                              StorageService storageService,
                              DownloadStateService downloadStateService,
                              AppProperties properties) {
        this.versionRepository = versionRepository;
        this.galleryRepository = galleryRepository;
        this.storageService = storageService;
        this.downloadStateService = downloadStateService;
        this.properties = properties;
    }

    public AppResponse toResponse(App app, CurrentUser user) {
        Optional<AppVersion> latestVersion = versionRepository.findTopByAppIdOrderByCreatedAtDesc(app.getId());

        List<String> galleryUrls = galleryRepository.findByAppIdOrderBySortOrderAsc(app.getId()).stream()
                .map(image -> storageService.presignGet(image.getFileKey(), properties.getDownloadTtl()))
                .toList();

        String logoUrl = storageService.presignGet(app.getLogoFileKey(), properties.getDownloadTtl());

        List<CategoryResponse> categories = app.getCategories().stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toCategoryResponse)
                .toList();

        return new AppResponse(
                app.getId(),
                app.getSlug(),
                app.getName(),
                app.getDescription(),
                app.getDeveloperId(),
                app.getStatus() == AppStatus.STUB ? null : app.getPrice(),
                categories,
                List.of(),
                logoUrl,
                galleryUrls,
                latestVersion.map(this::toVersionResponse).orElse(null),
                app.getCachedRating(),
                app.getCachedRatingCount(),
                app.getDownloadCount(),
                app.getStatus().name(),
                downloadStateService.resolve(app, user),
                app.getFirstPublishedAt(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }

    public AppVersionResponse toVersionResponse(AppVersion version) {
        return new AppVersionResponse(
                version.getId(),
                version.getVersionName(),
                version.getChangelog(),
                version.getCreatedAt()
        );
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getName(),
                category.getSvgIcon(),
                category.getCreatedAt()
        );
    }

    public GalleryImageResponse toGalleryResponse(GalleryImage image) {
        return new GalleryImageResponse(
                image.getId(),
                storageService.presignGet(image.getFileKey(), properties.getDownloadTtl()),
                image.getSortOrder()
        );
    }
}
