package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.config.AppProperties;
import io.github.anamespace.appservice.dto.AppVersionResponse;
import io.github.anamespace.appservice.dto.DeleteOldVersionsResponse;
import io.github.anamespace.appservice.dto.UploadInitResponse;
import io.github.anamespace.appservice.dto.VersionUpdateRequest;
import io.github.anamespace.appservice.dto.VersionUploadCommitRequest;
import io.github.anamespace.appservice.dto.VersionUploadInitRequest;
import io.github.anamespace.appservice.entity.App;
import io.github.anamespace.appservice.entity.AppStatus;
import io.github.anamespace.appservice.entity.AppVersion;
import io.github.anamespace.appservice.exception.BadRequestException;
import io.github.anamespace.appservice.exception.ConflictException;
import io.github.anamespace.appservice.exception.NotFoundException;
import io.github.anamespace.appservice.repository.AppVersionRepository;
import io.github.anamespace.appservice.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VersionService {

    private final AppService appService;
    private final AppVersionRepository versionRepository;
    private final StorageService storageService;
    private final AppResponseFactory responseFactory;
    private final AppProperties properties;

    public VersionService(AppService appService,
                          AppVersionRepository versionRepository,
                          StorageService storageService,
                          AppResponseFactory responseFactory,
                          AppProperties properties) {
        this.appService = appService;
        this.versionRepository = versionRepository;
        this.storageService = storageService;
        this.responseFactory = responseFactory;
        this.properties = properties;
    }

    public List<AppVersionResponse> list(UUID appId, CurrentUser user) {
        App app = appService.findById(appId);
        if (!appService.canManage(app, user) && app.getStatus() != AppStatus.PUBLISHED) {
            throw new NotFoundException("Приложение не найдено");
        }
        return versionRepository.findByAppIdOrderByCreatedAtDesc(app.getId()).stream()
                .map(responseFactory::toVersionResponse)
                .toList();
    }

    public UploadInitResponse initUpload(UUID appId, VersionUploadInitRequest request, CurrentUser user) {
        App app = appService.requireManagedApp(appId, user);
        if (request.replaceForVersionId() != null) {
            AppVersion version = requireLatestVersion(app, request.replaceForVersionId());
            if (!version.getVersionName().equals(request.versionName())) {
                throw new ConflictException("version_name нельзя изменить");
            }
        }
        String key = "pending/versions/" + app.getId() + "/" + UUID.randomUUID();
        StorageService.PresignedUpload upload = storageService.presignPut(key, properties.getUploadTtl());
        return new UploadInitResponse(upload.uploadUrl(), upload.fileKey(), upload.expiresAt());
    }

    @Transactional
    public AppVersionResponse commitUpload(UUID appId, VersionUploadCommitRequest request, CurrentUser user) {
        App app = appService.requireManagedApp(appId, user);
        String pendingPrefix = "pending/versions/" + app.getId() + "/";
        storageService.validateVersionFile(request.fileKey(), pendingPrefix);

        if (request.replaceForVersionId() != null) {
            AppVersion version = requireLatestVersion(app, request.replaceForVersionId());
            if (!version.getVersionName().equals(request.versionName())) {
                throw new ConflictException("version_name нельзя изменить");
            }
            String oldKey = version.getFileKey();
            String targetKey = versionKey(app, version.getId());
            storageService.move(request.fileKey(), targetKey);
            version.setFileKey(targetKey);
            versionRepository.save(version);
            if (oldKey != null && !oldKey.equals(targetKey)) {
                storageService.delete(oldKey);
            }
            return responseFactory.toVersionResponse(version);
        }

        if (versionRepository.existsByAppIdAndVersionName(app.getId(), request.versionName())) {
            throw new ConflictException("Версия с таким version_name уже существует");
        }

        UUID versionId = UUID.randomUUID();
        String targetKey = versionKey(app, versionId);
        storageService.move(request.fileKey(), targetKey);

        AppVersion version = new AppVersion();
        version.setId(versionId);
        version.setAppId(app.getId());
        version.setVersionName(request.versionName());
        version.setChangelog(request.changelog());
        version.setFileKey(targetKey);

        try {
            versionRepository.saveAndFlush(version);
        } catch (DataIntegrityViolationException exception) {
            storageService.delete(targetKey);
            throw new ConflictException("Версия с таким version_name уже существует");
        }
        return responseFactory.toVersionResponse(version);
    }

    @Transactional
    public AppVersionResponse update(UUID appId, UUID versionId, VersionUpdateRequest request, CurrentUser user) {
        App app = appService.requireManagedApp(appId, user);
        AppVersion version = requireVersion(app, versionId);
        requireLatest(app, version);

        if (request.versionName() != null && !request.versionName().equals(version.getVersionName())) {
            throw new ConflictException("version_name нельзя изменить");
        }
        if (request.changelog() != null) {
            version.setChangelog(request.changelog());
        }
        versionRepository.save(version);
        return responseFactory.toVersionResponse(version);
    }

    @Transactional
    public void delete(UUID appId, UUID versionId, CurrentUser user) {
        App app = appService.requireManagedApp(appId, user);
        AppVersion version = requireVersion(app, versionId);
        AppVersion latest = versionRepository.findTopByAppIdOrderByCreatedAtDesc(app.getId())
                .orElseThrow(() -> new NotFoundException("Версия не найдена"));
        if (latest.getId().equals(version.getId())) {
            throw new ConflictException("Нельзя удалить последнюю версию");
        }
        versionRepository.delete(version);
        storageService.delete(version.getFileKey());
    }

    @Transactional
    public DeleteOldVersionsResponse deleteOld(UUID appId, CurrentUser user) {
        App app = appService.requireManagedApp(appId, user);
        List<AppVersion> versions = versionRepository.findByAppIdOrderByCreatedAtDesc(app.getId());
        if (versions.size() <= 1) {
            return new DeleteOldVersionsResponse(0);
        }

        List<AppVersion> old = versions.subList(1, versions.size());
        for (AppVersion version : old) {
            storageService.delete(version.getFileKey());
        }
        versionRepository.deleteAll(old);
        return new DeleteOldVersionsResponse(old.size());
    }

    @Transactional
    public String replaceFile(UUID appId, UUID versionId, String fileKey, CurrentUser user) {
        App app = appService.requireManagedApp(appId, user);
        AppVersion version = requireVersion(app, versionId);
        requireLatest(app, version);
        storageService.validateVersionFile(fileKey, "pending/versions/" + app.getId() + "/");
        String oldKey = version.getFileKey();
        String targetKey = versionKey(app, version.getId());
        storageService.move(fileKey, targetKey);
        version.setFileKey(targetKey);
        versionRepository.save(version);
        if (oldKey != null && !oldKey.equals(targetKey)) {
            storageService.delete(oldKey);
        }
        return targetKey;
    }

    private AppVersion requireVersion(App app, UUID versionId) {
        return versionRepository.findByAppIdAndId(app.getId(), versionId)
                .orElseThrow(() -> new NotFoundException("Версия не найдена"));
    }

    private AppVersion requireLatestVersion(App app, UUID versionId) {
        AppVersion version = requireVersion(app, versionId);
        requireLatest(app, version);
        return version;
    }

    private void requireLatest(App app, AppVersion version) {
        AppVersion latest = versionRepository.findTopByAppIdOrderByCreatedAtDesc(app.getId())
                .orElseThrow(() -> new NotFoundException("Версия не найдена"));
        if (!latest.getId().equals(version.getId())) {
            throw new ConflictException("Изменять можно только последнюю версию");
        }
    }

    private String versionKey(App app, UUID versionId) {
        return "versions/" + app.getId() + "/" + versionId + "/file";
    }
}
