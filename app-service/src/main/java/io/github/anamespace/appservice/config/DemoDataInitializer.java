package io.github.anamespace.appservice.config;

import io.github.anamespace.appservice.entity.Category;
import io.github.anamespace.appservice.repository.CategoryRepository;
import io.github.anamespace.appservice.service.AppDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final CategoryRepository categoryRepository;

    public DemoDataInitializer(JdbcTemplate jdbcTemplate, CategoryRepository categoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedStub();
        seedCategories();
    }

    private void seedStub() {
        jdbcTemplate.update("""
                INSERT INTO apps (
                    id, slug, name, description, developer_id, logo_file_key, price, status,
                    first_published_at, cached_rating, cached_rating_count, download_count,
                    created_at, updated_at
                ) VALUES (?, 'deleted', 'Удалённое приложение', NULL, 0, NULL, 0.00, 'STUB',
                          NULL, 0.00, 0, 0, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, AppDeletionService.STUB_APP_ID);
        log.info("STUB application is present");
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> categories = List.of(
                category("games", "Игры", "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><rect x=\"3\" y=\"8\" width=\"18\" height=\"10\" rx=\"4\"/><path d=\"M8 13h4M10 11v4M16 12h.01M18 14h.01\"/></svg>"),
                category("tools", "Инструменты", "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M14.7 6.3a4 4 0 0 0-5 5L4 17l3 3 5.7-5.7a4 4 0 0 0 5-5l-2.4 2.4-2.3-.6-.6-2.3 2.3-2.5Z\"/></svg>"),
                category("productivity", "Продуктивность", "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><rect x=\"4\" y=\"4\" width=\"16\" height=\"16\" rx=\"3\"/><path d=\"M8 9h8M8 13h8M8 17h5\"/></svg>"),
                category("graphics", "Графика", "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M12 3a9 9 0 1 0 0 18h1.5a1.5 1.5 0 0 0 0-3H13a2 2 0 0 1 0-4h4a4 4 0 0 0 0-8h-5Z\"/><circle cx=\"7.5\" cy=\"10\" r=\"1\"/><circle cx=\"10\" cy=\"6.8\" r=\"1\"/><circle cx=\"7.5\" cy=\"14.5\" r=\"1\"/></svg>"),
                category("security", "Безопасность", "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M12 3 5 6v6c0 4.4 3 7.6 7 9 4-1.4 7-4.6 7-9V6l-7-3Z\"/><path d=\"m9 12 2 2 4-4\"/></svg>"),
                category("education", "Образование", "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"m3 9 9-5 9 5-9 5-9-5Z\"/><path d=\"M7 11.5V16c0 1.7 2.2 3 5 3s5-1.3 5-3v-4.5\"/></svg>")
        );
        categoryRepository.saveAll(categories);
        log.info("Seeded {} demo categories", categories.size());
    }

    private Category category(String slug, String name, String icon) {
        Category category = new Category();
        category.setSlug(slug);
        category.setName(name);
        category.setSvgIcon(icon);
        return category;
    }
}
