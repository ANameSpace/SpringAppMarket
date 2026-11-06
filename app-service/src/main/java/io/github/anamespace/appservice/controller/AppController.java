package io.github.anamespace.appservice.controller;

import io.github.anamespace.appservice.dto.AppResponse;
import io.github.anamespace.appservice.dto.CreateAppRequest;
import io.github.anamespace.appservice.dto.PageResponse;
import io.github.anamespace.appservice.dto.UpdateAppRequest;
import io.github.anamespace.appservice.security.CurrentUserService;
import io.github.anamespace.appservice.service.AppDeletionService;
import io.github.anamespace.appservice.service.AppService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apps")
public class AppController {

    private final AppService appService;
    private final AppDeletionService deletionService;
    private final CurrentUserService currentUserService;

    public AppController(AppService appService,
                         AppDeletionService deletionService,
                         CurrentUserService currentUserService) {
        this.appService = appService;
        this.deletionService = deletionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<AppResponse> create(@Valid @RequestBody CreateAppRequest request) {
        AppResponse response = appService.create(request, currentUserService.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slugOrId}")
    public AppResponse get(@PathVariable String slugOrId) {
        return appService.get(slugOrId, currentUserService.get());
    }

    @GetMapping
    public PageResponse<AppResponse> list(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) String purchaseState,
            @RequestParam(required = false) String status) {
        return appService.list(page, size, sort, q, categoryIds, purchaseState, status,
                currentUserService.get());
    }

    @PatchMapping("/{id}")
    public AppResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAppRequest request) {
        return appService.update(id, request, currentUserService.require());
    }

    @PostMapping("/{id}/publish")
    public AppResponse publish(@PathVariable UUID id) {
        return appService.publish(id, currentUserService.require());
    }

    @PostMapping("/{id}/unpublish")
    public AppResponse unpublish(@PathVariable UUID id) {
        return appService.unpublish(id, currentUserService.require());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deletionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
