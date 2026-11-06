package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.entity.App;
import io.github.anamespace.appservice.entity.AppStatus;
import io.github.anamespace.appservice.exception.BadRequestException;
import io.github.anamespace.appservice.exception.ForbiddenException;
import io.github.anamespace.appservice.repository.AppRepository;
import io.github.anamespace.appservice.repository.AppVersionRepository;
import io.github.anamespace.appservice.repository.GalleryImageRepository;
import io.github.anamespace.appservice.repository.UserAppDownloadRepository;
import io.github.anamespace.appservice.security.CurrentUser;
import io.github.anamespace.appservice.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
public class AppDeletionService {

    public static final UUID STUB_APP_ID = new UUID(0L, 0L);

    private final AppService appService;
    private final AppRepository appRepository;
    private final AppVersionRepository versionRepository;
    private final GalleryImageRepository galleryRepository;
    private final UserAppDownloadRepository downloadRepository;
    private final StorageService storageService;
    private final OrderServiceClient orderServiceClient;
    private final ReviewServiceClient reviewServiceClient;
    private final CurrentUserService currentUserService;
    private final TransactionTemplate transactionTemplate;

    public AppDeletionService(AppService appService,
                              AppRepository appRepository,
                              AppVersionRepository versionRepository,
                              GalleryImageRepository galleryRepository,
                              UserAppDownloadRepository downloadRepository,
                              StorageService storageService,
                              OrderServiceClient orderServiceClient,
                              ReviewServiceClient reviewServiceClient,
                              CurrentUserService currentUserService,
                              TransactionTemplate transactionTemplate) {
        this.appService = appService;
        this.appRepository = appRepository;
        this.versionRepository = versionRepository;
        this.galleryRepository = galleryRepository;
        this.downloadRepository = downloadRepository;
        this.storageService = storageService;
        this.orderServiceClient = orderServiceClient;
        this.reviewServiceClient = reviewServiceClient;
        this.currentUserService = currentUserService;
        this.transactionTemplate = transactionTemplate;
    }

    public void delete(UUID appId) {
        CurrentUser user = currentUserService.require();
        if (!user.isAdmin()) {
            throw new ForbiddenException("Удалять приложения может только администратор");
        }

        App app = appService.findById(appId);
        if (app.getStatus() == AppStatus.STUB) {
            throw new BadRequestException("Удалённое приложение нельзя удалить");
        }

        // Steps 2-6 are idempotent. Repeating a failed deletion is safe.
        orderServiceClient.replaceApp(app.getId(), STUB_APP_ID);
        orderServiceClient.deleteCartByApp(app.getId());
        reviewServiceClient.deleteByApp(app.getId());

        storageService.deletePrefix("media-drafts/" + app.getId() + "/");
        storageService.deletePrefix("media-published/" + app.getId() + "/");
        storageService.deletePrefix("versions/" + app.getId() + "/");
        storageService.deletePrefix("pending/versions/" + app.getId() + "/");
        storageService.deletePrefix("pending/media/" + app.getId() + "/");

        // Step 7: a single transaction for all remaining rows.
        transactionTemplate.executeWithoutResult(status -> {
            App managed = appRepository.findById(app.getId())
                    .orElseThrow(() -> new BadRequestException("Приложение уже удалено"));
            managed.getCategories().clear();
            downloadRepository.deleteByAppId(managed.getId());
            galleryRepository.deleteByAppId(managed.getId());
            versionRepository.deleteByAppId(managed.getId());
            appRepository.delete(managed);
        });
    }
}
