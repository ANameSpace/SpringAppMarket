package io.github.anamespace.appservice.repository;

import io.github.anamespace.appservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findBySlugIgnoreCase(String slug);

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
