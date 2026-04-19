-- ══════════════════════════════════════════════
-- V1__init_schema.sql — T-RESERVE ENGINE
-- PostgreSQL 16 | Flyway
-- ══════════════════════════════════════════════

-- Пользователи
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100),
    role          VARCHAR(20) DEFAULT 'USER'
                  CHECK (role IN ('USER', 'ADMIN')),
    version       BIGINT DEFAULT 0,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Площадки
CREATE TABLE venues (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(500),
    rows_count  INT NOT NULL,
    cols_count  INT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Места (физические кресла)
CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    venue_id    BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    row_label   VARCHAR(5) NOT NULL,
    seat_number INT NOT NULL,
    UNIQUE(venue_id, row_label, seat_number)
);

-- Мероприятия
CREATE TABLE events (
    id               BIGSERIAL PRIMARY KEY,
    venue_id         BIGINT NOT NULL REFERENCES venues(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    image_url        VARCHAR(500),
    age_restriction  VARCHAR(10),
    category         VARCHAR(50),
    duration_minutes INT,
    start_time       TIMESTAMPTZ NOT NULL,
    base_price       NUMERIC(10, 2) NOT NULL,
    status           VARCHAR(20) DEFAULT 'ACTIVE'
                     CHECK (status IN ('DRAFT', 'ACTIVE', 'CANCELLED', 'COMPLETED')),
    version          BIGINT DEFAULT 0,
    created_by       BIGINT REFERENCES users(id),
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Билеты (главная таблица)
CREATE TABLE tickets (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    seat_id         BIGINT NOT NULL REFERENCES seats(id),
    status          VARCHAR(20) DEFAULT 'AVAILABLE'
                    CHECK (status IN ('AVAILABLE', 'LOCKED', 'BOOKED')),
    price           NUMERIC(10, 2),
    user_id         BIGINT REFERENCES users(id),
    lock_expires_at TIMESTAMPTZ,
    booked_at       TIMESTAMPTZ,
    UNIQUE(event_id, seat_id)
);

-- ═══ Индексы ═══
CREATE INDEX idx_tickets_event_status ON tickets(event_id, status);
CREATE INDEX idx_tickets_user ON tickets(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_tickets_locked_expires ON tickets(lock_expires_at) WHERE status = 'LOCKED';
CREATE INDEX idx_events_status_time ON events(status, start_time);
CREATE INDEX idx_seats_venue ON seats(venue_id);
