package io.github.anamespace.appservice.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserAppDownloadId implements Serializable {

    private Long userId;
    private UUID appId;

    public UserAppDownloadId() {
    }

    public UserAppDownloadId(Long userId, UUID appId) {
        this.userId = userId;
        this.appId = appId;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserAppDownloadId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(appId, that.appId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, appId);
    }
}
