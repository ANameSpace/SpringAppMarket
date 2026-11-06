-- User profile fields for the existing SpringAppMarket demo database.
-- The script is intentionally idempotent: it runs on a fresh database and on
-- an existing one where Hibernate already created the original tables.

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email VARCHAR(255),
    display_name VARCHAR(100),
    avatar_color VARCHAR(7),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_color VARCHAR(7);
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

UPDATE users
SET email = lower(username) || '@springappmarket.local'
WHERE email IS NULL OR btrim(email) = '';

UPDATE users
SET display_name = username
WHERE display_name IS NULL OR btrim(display_name) = '';

UPDATE users
SET avatar_color = '#6B7280'
WHERE avatar_color IS NULL OR avatar_color !~ '^#[0-9A-Fa-f]{6}$';

UPDATE users
SET status = 'ACTIVE'
WHERE status IS NULL OR status NOT IN ('ACTIVE', 'BLOCKED');

UPDATE users SET created_at = NOW() WHERE created_at IS NULL;
UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;

ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ALTER COLUMN display_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN avatar_color SET NOT NULL;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);

INSERT INTO roles (name)
SELECT value
FROM (VALUES ('USER'), ('DEVELOPER'), ('ADMIN')) AS palette(value)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = palette.value);

-- Invariant: every account has the USER role. The admin seeder also grants it
-- on startup, but old rows must satisfy the invariant before the API is used.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'USER'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);
