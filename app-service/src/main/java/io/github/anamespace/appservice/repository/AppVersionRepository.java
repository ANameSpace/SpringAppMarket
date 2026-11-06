package io.github.anamespace.appservice.repository;

import io.github.anamespace.appservice.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppVersionRepository extends JpaRepository<AppVersion, UUID> {

    List<AppVersion> findByAppIdOrderByCreatedAtDesc(UUID appId);

    Optional<AppVersion> findTopByAppIdOrderByCreatedAtDesc(UUID appId);

    Optional<AppVersion> findByAppIdAndId(UUID appId, UUID id);

    boolean existsByAppIdAndVersionName(UUID appId, String versionName);

    long countByAppId(UUID appId);

    void deleteByAppId(UUID appId);
}
