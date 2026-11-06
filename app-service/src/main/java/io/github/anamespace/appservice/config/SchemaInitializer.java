package io.github.anamespace.appservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate creates the tables, but it cannot express the case-insensitive
 * unique indexes required by the specification. PostgreSQL functional indexes
 * are therefore created here. The statements are idempotent, so the service can
 * be restarted safely.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        create("CREATE UNIQUE INDEX IF NOT EXISTS ux_apps_slug_lower ON apps (lower(slug))");
        create("CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_slug_lower ON categories (lower(slug))");
        create("CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_name_lower ON categories (lower(name))");
        create("CREATE UNIQUE INDEX IF NOT EXISTS ux_app_versions_app_version ON app_versions (app_id, version_name)");
        create("CREATE INDEX IF NOT EXISTS ix_apps_developer ON apps (developer_id)");
        create("CREATE INDEX IF NOT EXISTS ix_apps_status ON apps (status)");
    }

    private void create(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (RuntimeException exception) {
            log.warn("Could not apply schema statement [{}]: {}", sql, exception.getMessage());
        }
    }
}
