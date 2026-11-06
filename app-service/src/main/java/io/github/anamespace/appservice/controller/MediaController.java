package io.github.anamespace.appservice.controller;

import io.github.anamespace.appservice.dto.AppResponse;
import io.github.anamespace.appservice.dto.GalleryImageResponse;
import io.github.anamespace.appservice.dto.GalleryUploadCommitRequest;
import io.github.anamespace.appservice.dto.LogoUploadCommitRequest;
import io.github.anamespace.appservice.dto.UploadInitResponse;
import io.github.anamespace.appservice.security.CurrentUserService;
import io.github.anamespace.appservice.service.MediaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apps/{appId}")
public class MediaController {

    private final MediaService mediaService;
    private final CurrentUserService currentUserService;

    public MediaController(MediaService mediaService,
                           CurrentUserService currentUserService) {
        this.mediaService = mediaService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/logo/upload-init")
    public UploadInitResponse initLogo(@PathVariable UUID appId) {
        return mediaService.initLogoUpload(appId, currentUserService.require());
    }

    @PostMapping("/logo/upload-commit")
    public AppResponse commitLogo(@PathVariable UUID appId,
                                  @Valid @RequestBody LogoUploadCommitRequest request) {
        return mediaService.commitLogo(appId, request.fileKey(), currentUserService.require());
    }

    @PostMapping("/gallery/upload-init")
    public UploadInitResponse initGallery(@PathVariable UUID appId) {
        return mediaService.initGalleryUpload(appId, currentUserService.require());
    }

    @PostMapping("/gallery/upload-commit")
    public GalleryImageResponse commitGallery(@PathVariable UUID appId,
                                              @Valid @RequestBody GalleryUploadCommitRequest request) {
        return mediaService.commitGallery(appId, request, currentUserService.require());
    }

    @GetMapping("/gallery")
    public List<GalleryImageResponse> gallery(@PathVariable UUID appId) {
        return mediaService.gallery(appId);
    }

    @DeleteMapping("/gallery/{imageId}")
    public ResponseEntity<Void> deleteGallery(@PathVariable UUID appId, @PathVariable UUID imageId) {
        mediaService.deleteGallery(appId, imageId, currentUserService.require());
        return ResponseEntity.noContent().build();
    }
}
