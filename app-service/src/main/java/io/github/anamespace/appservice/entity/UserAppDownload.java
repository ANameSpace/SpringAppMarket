package io.github.anamespace.appservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_app_downloads")
@IdClass(UserAppDownloadId.class)
public class UserAppDownload {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "last_version_id")
    private UUID lastVersionId;

    @Column(name = "last_downloaded_at")
    private Instant lastDownloadedAt;

    public UserAppDownload() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    public UUID getLastVersionId() {
        return lastVersionId;
    }

    public void setLastVersionId(UUID lastVersionId) {
        this.lastVersionId = lastVersionId;
    }

    public Instant getLastDownloadedAt() {
        return lastDownloadedAt;
    }

    public void setLastDownloadedAt(Instant lastDownloadedAt) {
        this.lastDownloadedAt = lastDownloadedAt;
    }
}
