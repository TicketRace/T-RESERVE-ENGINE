-- ══════════════════════════════════════════════
-- V8__add_oauth2_fields.sql — Google OAuth2 support
-- ══════════════════════════════════════════════

-- Google-пользователи не имеют пароля — делаем nullable
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Провайдер авторизации: LOCAL (email+password) или GOOGLE
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- Google sub (уникальный идентификатор пользователя в Google)
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

-- Аватар из Google profile
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- Обновляем CHECK чтобы auth_provider был валидным
ALTER TABLE users ADD CONSTRAINT check_auth_provider
    CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));
