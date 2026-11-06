package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.dto.AppResponse;
import io.github.anamespace.appservice.dto.CreateAppRequest;
import io.github.anamespace.appservice.dto.PageResponse;
import io.github.anamespace.appservice.dto.UpdateAppRequest;
import io.github.anamespace.appservice.entity.App;
import io.github.anamespace.appservice.entity.AppStatus;
import io.github.anamespace.appservice.entity.Category;
import io.github.anamespace.appservice.exception.BadRequestException;
import io.github.anamespace.appservice.exception.ConflictException;
import io.github.anamespace.appservice.exception.ForbiddenException;
import io.github.anamespace.appservice.exception.NotFoundException;
import io.github.anamespace.appservice.exception.UnauthorizedException;
import io.github.anamespace.appservice.repository.AppRepository;
import io.github.anamespace.appservice.repository.AppVersionRepository;
import io.github.anamespace.appservice.repository.CategoryRepository;
import io.github.anamespace.appservice.security.CurrentUser;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AppService {

    private final AppRepository appRepository;
    private final AppVersionRepository versionRepository;
    private final CategoryRepository categoryRepository;
    private final MediaService mediaService;
    private final AppResponseFactory responseFactory;

    public AppService(AppRepository appRepository,
                      AppVersionRepository versionRepository,
                      CategoryRepository categoryRepository,
                      MediaService mediaService,
                      AppResponseFactory responseFactory) {
        this.appRepository = appRepository;
        this.versionRepository = versionRepository;
        this.categoryRepository = categoryRepository;
        this.mediaService = mediaService;
        this.responseFactory = responseFactory;
    }

    @Transactional
    public AppResponse create(CreateAppRequest request, CurrentUser user) {
        CurrentUser current = requireUser(user);
        if (!current.isDeveloper() && !current.isAdmin()) {
            throw new ForbiddenException("Создавать приложения могут только разработчики");
        }
        Long developerId = current.id();
        if (developerId == null) {
            throw new UnauthorizedException("В токене отсутствует идентификатор пользователя");
        }
        if (appRepository.existsBySlugIgnoreCase(request.slug())) {
            throw new ConflictException("Slug уже занят");
        }

        App app = new App();
        app.setName(request.name().trim());
        app.setSlug(request.slug());
        app.setDescription(normalize(request.description()));
        app.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
        app.setDeveloperId(developerId);
        app.setStatus(AppStatus.DRAFT);
        app.setCategories(resolveCategories(request.categoryIds()));

        try {
            appRepository.saveAndFlush(app);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Slug уже занят");
        }
        return responseFactory.toResponse(app, current);
    }

    public AppResponse get(String slugOrId, CurrentUser user) {
        App app = find(slugOrId);
        if (app.getStatus() == AppStatus.DRAFT && !canManage(app, user)) {
            throw new NotFoundException("Приложение не найдено");
        }
        return responseFactory.toResponse(app, user);
    }

    public PageResponse<AppResponse> list(int page,
                                          int size,
                                          String sort,
                                          String q,
                                          List<UUID> categoryIds,
                                          String purchaseState,
                                          String status,
                                          CurrentUser user) {
        validatePagination(page, size);
        if (purchaseState != null && !purchaseState.isBlank()) {
            if (user == null) {
                throw new UnauthorizedException("Требуется авторизация");
            }
            if (!purchaseState.equals("purchased") && !purchaseState.equals("not_purchased")) {
                throw new BadRequestException("purchaseState должен быть purchased или not_purchased");
            }
            // The order-service stub reports every app as purchased.
            if (purchaseState.equals("not_purchased")) {
                return PageResponse.empty(page, size);
            }
        }

        Specification<App> specification = buildSpecification(q, categoryIds, status, user);
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Page<App> apps = appRepository.findAll(specification, pageable);

        List<AppResponse> content = new ArrayList<>(apps.getNumberOfElements());
        for (App app : apps.getContent()) {
            content.add(responseFactory.toResponse(app, user));
        }
        return new PageResponse<>(
                content,
                apps.getNumber(),
                apps.getSize(),
                apps.getTotalElements(),
                apps.getTotalPages()
        );
    }

    @Transactional
    public AppResponse update(UUID appId, UpdateAppRequest request, CurrentUser user) {
        CurrentUser current = requireUser(user);
        App app = requireManagedApp(appId, current);

        if (request.name() != null) {
            app.setName(request.name().trim());
        }
        if (request.description() != null) {
            app.setDescription(normalize(request.description()));
        }
        if (request.price() != null) {
            app.setPrice(request.price());
        }
        if (request.categoryIds() != null) {
            app.setCategories(resolveCategories(request.categoryIds()));
        }
        if (request.slug() != null && !request.slug().equals(app.getSlug())) {
            if (app.getFirstPublishedAt() != null) {
                throw new ConflictException("Slug нельзя изменить после первой публикации");
            }
            if (appRepository.existsBySlugIgnoreCaseAndIdNot(request.slug(), app.getId())) {
                throw new ConflictException("Slug уже занят");
            }
            app.setSlug(request.slug());
        }

        try {
            appRepository.saveAndFlush(app);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Slug уже занят");
        }
        return responseFactory.toResponse(app, current);
    }

    @Transactional
    public AppResponse publish(UUID appId, CurrentUser user) {
        CurrentUser current = requireUser(user);
        App app = requireManagedApp(appId, current);
        if (app.getStatus() != AppStatus.DRAFT) {
            throw new BadRequestException("Публиковать можно только черновик");
        }
        if (versionRepository.countByAppId(app.getId()) == 0) {
            throw new BadRequestException("Нужна хотя бы одна версия");
        }

        app.setStatus(AppStatus.PUBLISHED);
        if (app.getFirstPublishedAt() == null) {
            app.setFirstPublishedAt(Instant.now());
        }
        appRepository.save(app);
        mediaService.moveToPublished(app);
        return responseFactory.toResponse(app, current);
    }

    @Transactional
    public AppResponse unpublish(UUID appId, CurrentUser user) {
        CurrentUser current = requireUser(user);
        App app = requireManagedApp(appId, current);
        if (app.getStatus() != AppStatus.PUBLISHED) {
            throw new BadRequestException("Снять с публикации можно только опубликованное приложение");
        }

        app.setStatus(AppStatus.DRAFT);
        appRepository.save(app);
        mediaService.moveToDrafts(app);
        return responseFactory.toResponse(app, current);
    }

    public App find(String slugOrId) {
        if (slugOrId == null || slugOrId.isBlank()) {
            throw new NotFoundException("Приложение не найдено");
        }
        try {
            UUID id = UUID.fromString(slugOrId);
            return appRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Приложение не найдено"));
        } catch (IllegalArgumentException ignored) {
            return appRepository.findBySlugIgnoreCase(slugOrId)
                    .orElseThrow(() -> new NotFoundException("Приложение не найдено"));
        }
    }

    public App findById(UUID appId) {
        return appRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Приложение не найдено"));
    }

    public App requireManagedApp(UUID appId, CurrentUser user) {
        App app = findById(appId);
        if (app.getStatus() == AppStatus.STUB) {
            throw new BadRequestException("Удалённое приложение нельзя изменять");
        }
        if (!canManage(app, user)) {
            throw new ForbiddenException("Недостаточно прав для изменения приложения");
        }
        return app;
    }

    public boolean canManage(App app, CurrentUser user) {
        return user != null && (user.isAdmin() || user.isOwner(app));
    }

    private CurrentUser requireUser(CurrentUser user) {
        if (user == null) {
            throw new UnauthorizedException("Требуется авторизация");
        }
        return user;
    }

    private Specification<App> buildSpecification(String q, List<UUID> categoryIds, String status, CurrentUser user) {
        boolean admin = user != null && user.isAdmin();
        Specification<App> specification = (root, query, cb) -> cb.conjunction();

        if (status != null && !status.isBlank()) {
            AppStatus requested;
            try {
                requested = AppStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("status должен быть DRAFT или PUBLISHED");
            }
            if (requested == AppStatus.STUB) {
                throw new BadRequestException("STUB нельзя запросить в списке");
            }
            if (!admin) {
                if (user == null) {
                    throw new UnauthorizedException("Требуется авторизация");
                }
                Long developerId = user.id();
                specification = specification.and((root, query, cb) ->
                        cb.equal(root.get("developerId"), developerId));
            }
            AppStatus finalRequested = requested;
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), finalRequested));
        } else if (admin) {
            specification = specification.and((root, query, cb) ->
                    cb.notEqual(root.get("status"), AppStatus.STUB));
        } else {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), AppStatus.PUBLISHED));
        }

        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("slug")), pattern)
            ));
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<UUID> ids = categoryIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
            if (!ids.isEmpty()) {
                specification = specification.and((root, query, cb) -> {
                    Subquery<UUID> subquery = query.subquery(UUID.class);
                    Root<App> appRoot = subquery.from(App.class);
                    Join<App, Category> join = appRoot.join("categories");
                    subquery.select(appRoot.get("id"));
                    subquery.where(join.get("id").in(ids));
                    return root.get("id").in(subquery);
                });
            }
        }

        return specification;
    }

    private Sort resolveSort(String sort) {
        String value = sort == null || sort.isBlank() ? "rating_desc" : sort;
        return switch (value) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createdAt"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("createdAt"));
            case "name_asc" -> Sort.by(Sort.Order.asc("name").ignoreCase());
            case "name_desc" -> Sort.by(Sort.Order.desc("name").ignoreCase());
            case "rating_desc" -> Sort.by(
                    Sort.Order.desc("cachedRating"),
                    Sort.Order.desc("downloadCount"),
                    Sort.Order.desc("createdAt")
            );
            case "downloads_desc" -> Sort.by(
                    Sort.Order.desc("downloadCount"),
                    Sort.Order.desc("createdAt")
            );
            default -> throw new BadRequestException("Недопустимое значение sort");
        };
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page должен быть неотрицательным");
        }
        if (size < 1 || size > 50) {
            throw new BadRequestException("size должен быть от 1 до 50");
        }
    }

    private Set<Category> resolveCategories(List<UUID> categoryIds) {
        Set<Category> categories = new LinkedHashSet<>();
        if (categoryIds == null || categoryIds.isEmpty()) {
            return categories;
        }
        if (categoryIds.size() > 3) {
            throw new BadRequestException("Не более 3 категорий на приложение");
        }
        for (UUID categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BadRequestException("Категория не найдена: " + categoryId));
            categories.add(category);
        }
        return categories;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
