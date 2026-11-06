package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.config.AppProperties;
import io.github.anamespace.appservice.dto.DownloadResponse;
import io.github.anamespace.appservice.entity.App;
import io.github.anamespace.appservice.entity.AppStatus;
import io.github.anamespace.appservice.entity.AppVersion;
import io.github.anamespace.appservice.entity.UserAppDownload;
import io.github.anamespace.appservice.exception.BadRequestException;
import io.github.anamespace.appservice.exception.ForbiddenException;
import io.github.anamespace.appservice.exception.NotFoundException;
import io.github.anamespace.appservice.repository.AppRepository;
import io.github.anamespace.appservice.repository.AppVersionRepository;
import io.github.anamespace.appservice.repository.UserAppDownloadRepository;
import io.github.anamespace.appservice.security.CurrentUser;
import io.github.anamespace.appservice.security.CurrentUserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class DownloadService {

    private final AppService appService;
    private final AppVersionRepository versionRepository;
    private final UserAppDownloadRepository downloadRepository;
    private final AppRepository appRepository;
    private final StorageService storageService;
    private final OrderServiceClient orderServiceClient;
    private final CurrentUserService currentUserService;
    private final AppProperties properties;

    public DownloadService(AppService appService,
                           AppVersionRepository versionRepository,
                           UserAppDownloadRepository downloadRepository,
                           AppRepository appRepository,
                           StorageService storageService,
                           OrderServiceClient orderServiceClient,
                           CurrentUserService currentUserService,
                           AppProperties properties) {
        this.appService = appService;
        this.versionRepository = versionRepository;
        this.downloadRepository = downloadRepository;
        this.appRepository = appRepository;
        this.storageService = storageService;
        this.orderServiceClient = orderServiceClient;
        this.currentUserService = currentUserService;
        this.properties = properties;
    }

    @Transactional
    public DownloadResponse download(String slugOrId) {
        CurrentUser user = currentUserService.require();
        App app = appService.find(slugOrId);

        if (app.getStatus() == AppStatus.STUB) {
            throw new BadRequestException("Удалённое приложение нельзя скачать");
        }
        if (user.isAdmin()) {
            throw new ForbiddenException("Администратор не может скачивать приложения");
        }

        boolean owner = user.isOwner(app);
        if (!owner) {
            if (app.getStatus() != AppStatus.PUBLISHED) {
                throw new NotFoundException("Приложение не найдено");
            }
            if (!orderServiceClient.hasCompletedPurchase(user.id(), app.getId())) {
                throw new ForbiddenException("Приложение не куплено");
            }
        }

        AppVersion latest = versionRepository.findTopByAppIdOrderByCreatedAtDesc(app.getId())
                .orElseThrow(() -> new BadRequestException("Нет доступных версий"));

        String contentType = storageService.contentType(latest.getFileKey());
        String extension = StorageService.extensionForContentType(contentType);
        String fileName = safe(app.getSlug()) + "-" + safe(latest.getVersionName()) + "." + extension;
        Instant expiresAt = Instant.now().plus(properties.getDownloadTtl());
        String url = storageService.presignAttachment(
                latest.getFileKey(),
                fileName,
                properties.getDownloadTtl());

        UserAppDownload download = downloadRepository
                .findByUserIdAndAppId(user.id(), app.getId())
                .orElseGet(() -> {
                    UserAppDownload created = new UserAppDownload();
                    created.setUserId(user.id());
                    created.setAppId(app.getId());
                    return created;
                });
        download.setLastVersionId(latest.getId());
        download.setLastDownloadedAt(Instant.now());
        downloadRepository.save(download);

        app.setDownloadCount(app.getDownloadCount() + 1);
        appRepository.save(app);

        return new DownloadResponse(url, fileName, expiresAt);
    }

    private String safe(String value) {
        return value == null ? "app" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
