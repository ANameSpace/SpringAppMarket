package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.entity.App;
import io.github.anamespace.appservice.entity.AppStatus;
import io.github.anamespace.appservice.entity.AppVersion;
import io.github.anamespace.appservice.entity.DownloadState;
import io.github.anamespace.appservice.entity.UserAppDownload;
import io.github.anamespace.appservice.repository.AppVersionRepository;
import io.github.anamespace.appservice.repository.UserAppDownloadRepository;
import io.github.anamespace.appservice.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DownloadStateService {

    private final AppVersionRepository versionRepository;
    private final UserAppDownloadRepository downloadRepository;
    private final OrderServiceClient orderServiceClient;

    public DownloadStateService(AppVersionRepository versionRepository,
                                UserAppDownloadRepository downloadRepository,
                                OrderServiceClient orderServiceClient) {
        this.versionRepository = versionRepository;
        this.downloadRepository = downloadRepository;
        this.orderServiceClient = orderServiceClient;
    }

    public DownloadState resolve(App app, CurrentUser user) {
        if (app.getStatus() == AppStatus.STUB) {
            return DownloadState.NOT_AVAILABLE;
        }
        if (user == null || user.id() == null) {
            return null;
        }
        if (user.isAdmin()) {
            return DownloadState.NOT_AVAILABLE_FOR_ADMIN;
        }

        Optional<AppVersion> latest = versionRepository.findTopByAppIdOrderByCreatedAtDesc(app.getId());
        Optional<UserAppDownload> download = downloadRepository.findByUserIdAndAppId(user.id(), app.getId());

        if (user.isOwner(app)) {
            if (download.isEmpty()) {
                return DownloadState.OWN_APP_NOT_DOWNLOADED;
            }
            if (latest.isPresent() && latest.get().getId().equals(download.get().getLastVersionId())) {
                return DownloadState.OWN_APP_UP_TO_DATE;
            }
            return DownloadState.OWN_APP_UPDATE_AVAILABLE;
        }

        if (!orderServiceClient.hasCompletedPurchase(user.id(), app.getId())) {
            return DownloadState.NOT_PURCHASED;
        }
        if (download.isEmpty()) {
            return DownloadState.PURCHASED_NOT_DOWNLOADED;
        }
        if (latest.isPresent() && latest.get().getId().equals(download.get().getLastVersionId())) {
            return DownloadState.UP_TO_DATE;
        }
        return DownloadState.UPDATE_AVAILABLE;
    }
}
