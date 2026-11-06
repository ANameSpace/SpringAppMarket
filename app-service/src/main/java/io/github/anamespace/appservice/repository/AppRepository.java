package io.github.anamespace.appservice.repository;

import io.github.anamespace.appservice.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AppRepository extends JpaRepository<App, UUID>, JpaSpecificationExecutor<App> {

    Optional<App> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);
}
