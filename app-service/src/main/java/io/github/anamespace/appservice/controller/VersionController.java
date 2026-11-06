package io.github.anamespace.appservice.controller;

import io.github.anamespace.appservice.dto.AppVersionResponse;
import io.github.anamespace.appservice.dto.DeleteOldVersionsResponse;
import io.github.anamespace.appservice.dto.UploadInitResponse;
import io.github.anamespace.appservice.dto.VersionUpdateRequest;
import io.github.anamespace.appservice.dto.VersionUploadCommitRequest;
import io.github.anamespace.appservice.dto.VersionUploadInitRequest;
import io.github.anamespace.appservice.security.CurrentUserService;
import io.github.anamespace.appservice.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apps/{appId}/versions")
public class VersionController {

    private final VersionService versionService;
    private final CurrentUserService currentUserService;

    public VersionController(VersionService versionService,
                             CurrentUserService currentUserService) {
        this.versionService = versionService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AppVersionResponse> list(@PathVariable UUID appId) {
        return versionService.list(appId, currentUserService.get());
    }

    @PostMapping("/upload-init")
    public UploadInitResponse initUpload(@PathVariable UUID appId,
                                         @Valid @RequestBody VersionUploadInitRequest request) {
        return versionService.initUpload(appId, request, currentUserService.require());
    }

    @PostMapping("/upload-commit")
    public AppVersionResponse commitUpload(@PathVariable UUID appId,
                                           @Valid @RequestBody VersionUploadCommitRequest request) {
        return versionService.commitUpload(appId, request, currentUserService.require());
    }

    @PatchMapping("/{versionId}")
    public AppVersionResponse update(@PathVariable UUID appId,
                                     @PathVariable UUID versionId,
                                     @Valid @RequestBody VersionUpdateRequest request) {
        return versionService.update(appId, versionId, request, currentUserService.require());
    }

    @DeleteMapping("/old")
    public DeleteOldVersionsResponse deleteOld(@PathVariable UUID appId) {
        return versionService.deleteOld(appId, currentUserService.require());
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID appId, @PathVariable UUID versionId) {
        versionService.delete(appId, versionId, currentUserService.require());
        return ResponseEntity.noContent().build();
    }
}
