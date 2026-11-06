package io.github.anamespace.appservice.repository;

import io.github.anamespace.appservice.entity.UserAppDownload;
import io.github.anamespace.appservice.entity.UserAppDownloadId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAppDownloadRepository extends JpaRepository<UserAppDownload, UserAppDownloadId> {

    Optional<UserAppDownload> findByUserIdAndAppId(Long userId, UUID appId);

    void deleteByAppId(UUID appId);
}
