package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.config.AppProperties;
import io.github.anamespace.appservice.dto.AppResponse;
import io.github.anamespace.appservice.dto.GalleryImageResponse;
import io.github.anamespace.appservice.dto.GalleryUploadCommitRequest;
import io.github.anamespace.appservice.dto.UploadInitResponse;
import io.github.anamespace.appservice.entity.App;
import io.github.anamespace.appservice.entity.AppStatus;
import io.github.anamespace.appservice.entity.GalleryImage;
import io.github.anamespace.appservice.exception.BadRequestException;
import io.github.anamespace.appservice.exception.ForbiddenException;
import io.github.anamespace.appservice.exception.NotFoundException;
import io.github.anamespace.appservice.repository.AppRepository;
import io.github.anamespace.appservice.repository.GalleryImageRepository;
import io.github.anamespace.appservice.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MediaService {

    private static final String MEDIA_DRAFTS = "media-drafts";
    private static final String MEDIA_PUBLISHED = "media-published";

    private final AppRepository appRepository;
    private final GalleryImageRepository galleryRepository;
    private final StorageService storageService;
    private final AppResponseFactory responseFactory;
    private final AppProperties properties;

    public MediaService(AppRepository appRepository,
                        GalleryImageRepository galleryRepository,
                        StorageService storageService,
                        AppResponseFactory responseFactory,
                        AppProperties properties) {
        this.appRepository = appRepository;
        this.galleryRepository = galleryRepository;
        this.storageService = storageService;
        this.responseFactory = responseFactory;
        this.properties = properties;
    }

    public UploadInitResponse initLogoUpload(UUID appId, CurrentUser user) {
        App app = requireManagedApp(appId, user);
        String key = pendingKey(app);
        StorageService.PresignedUpload upload = storageService.presignPut(key, properties.getUploadTtl());
        return new UploadInitResponse(upload.uploadUrl(), upload.fileKey(), upload.expiresAt());
    }

    @Transactional
    public AppResponse commitLogo(UUID appId, String fileKey, CurrentUser user) {
        App app = requireManagedApp(appId, user);
        String contentType = storageService.validateImageFile(fileKey, pendingPrefix(app));
        String extension = StorageService.extensionForContentType(contentType);
        String targetKey = mediaRoot(app) + "/" + app.getId() + "/logo." + extension;

        storageService.move(fileKey, targetKey);
        String oldKey = app.getLogoFileKey();
        app.setLogoFileKey(targetKey);
        appRepository.save(app);

        if (oldKey != null && !oldKey.equals(targetKey)) {
            storageService.delete(oldKey);
        }
        return responseFactory.toResponse(app, user);
    }

    public UploadInitResponse initGalleryUpload(UUID appId, CurrentUser user) {
        App app = requireManagedApp(appId, user);
        String key = pendingKey(app);
        StorageService.PresignedUpload upload = storageService.presignPut(key, properties.getUploadTtl());
        return new UploadInitResponse(upload.uploadUrl(), upload.fileKey(), upload.expiresAt());
    }

    @Transactional
    public GalleryImageResponse commitGallery(UUID appId, GalleryUploadCommitRequest request, CurrentUser user) {
        App app = requireManagedApp(appId, user);
        long current = galleryRepository.countByAppId(app.getId());
        if (current >= 10) {
            throw new BadRequestException("В галерее уже 10 изображений");
        }
        if (request.sortOrder() < 0 || request.sortOrder() > 9) {
            throw new BadRequestException("sortOrder должен быть от 0 до 9");
        }

        String contentType = storageService.validateImageFile(request.fileKey(), pendingPrefix(app));
        String extension = StorageService.extensionForContentType(contentType);
        String targetKey = mediaRoot(app) + "/" + app.getId() + "/gallery/" + UUID.randomUUID() + "." + extension;

        storageService.move(request.fileKey(), targetKey);

        GalleryImage image = new GalleryImage();
        image.setAppId(app.getId());
        image.setFileKey(targetKey);
        image.setSortOrder(request.sortOrder());
        galleryRepository.save(image);

        return responseFactory.toGalleryResponse(image);
    }

    @Transactional
    public void deleteGallery(UUID appId, UUID imageId, CurrentUser user) {
        App app = requireManagedApp(appId, user);
        GalleryImage image = galleryRepository.findByAppIdAndId(app.getId(), imageId)
                .orElseThrow(() -> new NotFoundException("Изображение не найдено"));
        galleryRepository.delete(image);
        storageService.delete(image.getFileKey());
    }

    public List<GalleryImageResponse> gallery(UUID appId) {
        return galleryRepository.findByAppIdOrderBySortOrderAsc(appId).stream()
                .map(responseFactory::toGalleryResponse)
                .toList();
    }

    @Transactional
    public void moveToPublished(App app) {
        moveMedia(app, MEDIA_DRAFTS, MEDIA_PUBLISHED);
    }

    @Transactional
    public void moveToDrafts(App app) {
        moveMedia(app, MEDIA_PUBLISHED, MEDIA_DRAFTS);
    }

    private void moveMedia(App app, String fromRoot, String toRoot) {
        if (app.getLogoFileKey() != null && app.getLogoFileKey().startsWith(fromRoot + "/")) {
            String target = toRoot + app.getLogoFileKey().substring(fromRoot.length());
            storageService.move(app.getLogoFileKey(), target);
            app.setLogoFileKey(target);
        }

        for (GalleryImage image : galleryRepository.findByAppIdOrderBySortOrderAsc(app.getId())) {
            if (image.getFileKey() != null && image.getFileKey().startsWith(fromRoot + "/")) {
                String target = toRoot + image.getFileKey().substring(fromRoot.length());
                storageService.move(image.getFileKey(), target);
                image.setFileKey(target);
                galleryRepository.save(image);
            }
        }
        appRepository.save(app);
    }

    private App requireManagedApp(UUID appId, CurrentUser user) {
        App app = appRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Приложение не найдено"));
        if (app.getStatus() == AppStatus.STUB) {
            throw new BadRequestException("Удалённое приложение нельзя изменять");
        }
        if (!user.isAdmin() && !user.isOwner(app)) {
            throw new ForbiddenException("Недостаточно прав для изменения приложения");
        }
        return app;
    }

    private String mediaRoot(App app) {
        return app.getStatus() == AppStatus.PUBLISHED ? MEDIA_PUBLISHED : MEDIA_DRAFTS;
    }

    private String pendingPrefix(App app) {
        return "pending/media/" + app.getId() + "/";
    }

    private String pendingKey(App app) {
        return pendingPrefix(app) + UUID.randomUUID();
    }
}
