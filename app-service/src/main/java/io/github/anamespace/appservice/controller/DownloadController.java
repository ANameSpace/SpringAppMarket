package io.github.anamespace.appservice.controller;

import io.github.anamespace.appservice.dto.DownloadResponse;
import io.github.anamespace.appservice.service.DownloadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/apps")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/{slugOrId}/download")
    public DownloadResponse download(@PathVariable String slugOrId) {
        return downloadService.download(slugOrId);
    }
}
