package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.dto.CategoryRequest;
import io.github.anamespace.appservice.dto.CategoryResponse;
import io.github.anamespace.appservice.dto.UpdateCategoryRequest;
import io.github.anamespace.appservice.entity.Category;
import io.github.anamespace.appservice.exception.BadRequestException;
import io.github.anamespace.appservice.exception.ConflictException;
import io.github.anamespace.appservice.exception.NotFoundException;
import io.github.anamespace.appservice.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private static final int MAX_SVG_BYTES = 64 * 1024;

    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AppResponseFactory responseFactory;

    public CategoryService(CategoryRepository categoryRepository,
                           JdbcTemplate jdbcTemplate,
                           AppResponseFactory responseFactory) {
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.responseFactory = responseFactory;
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(responseFactory::toCategoryResponse)
                .toList();
    }

    public CategoryResponse get(String idOrSlugOrName) {
        return responseFactory.toCategoryResponse(find(idOrSlugOrName));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = request.slug().trim();
        String name = request.name().trim();
        validateSvg(request.svgIcon());

        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictException("Категория с таким slug уже существует");
        }
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Категория с таким name уже существует");
        }

        Category category = new Category();
        category.setSlug(slug);
        category.setName(name);
        category.setSvgIcon(request.svgIcon());
        categoryRepository.save(category);
        return responseFactory.toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        if (request.slug() != null) {
            String slug = request.slug().trim();
            if (!slug.equalsIgnoreCase(category.getSlug())
                    && categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
                throw new ConflictException("Категория с таким slug уже существует");
            }
            category.setSlug(slug);
        }
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("name обязателен");
            }
            if (!name.equalsIgnoreCase(category.getName())
                    && categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new ConflictException("Категория с таким name уже существует");
            }
            category.setName(name);
        }
        if (request.svgIcon() != null) {
            validateSvg(request.svgIcon());
            category.setSvgIcon(request.svgIcon());
        }

        categoryRepository.save(category);
        return responseFactory.toCategoryResponse(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));
        jdbcTemplate.update("delete from app_categories where category_id = ?", id);
        categoryRepository.delete(category);
    }

    private Category find(String idOrSlugOrName) {
        if (idOrSlugOrName == null || idOrSlugOrName.isBlank()) {
            throw new NotFoundException("Категория не найдена");
        }
        try {
            UUID id = UUID.fromString(idOrSlugOrName);
            return categoryRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
        } catch (IllegalArgumentException ignored) {
            return categoryRepository.findBySlugIgnoreCase(idOrSlugOrName)
                    .or(() -> categoryRepository.findByNameIgnoreCase(idOrSlugOrName))
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
        }
    }

    private void validateSvg(String svgIcon) {
        if (svgIcon != null && svgIcon.getBytes(StandardCharsets.UTF_8).length > MAX_SVG_BYTES) {
            throw new BadRequestException("svgIcon не должен превышать 64 КБ");
        }
    }
}
