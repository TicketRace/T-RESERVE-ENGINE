-- ══════════════════════════════════════════════
-- V2__seed_data.sql — Тестовые данные
-- ══════════════════════════════════════════════

-- Админ (пароль: admin123, bcrypt hash)
INSERT INTO users (email, password_hash, name, role)
VALUES ('admin@treserve.com', '$2a$12$LJ3tUwqZxvGKoMYuKEj8duKq4dFMFkP/nx/c4JhCSlPOJQ9BXrVfe', 'Admin', 'ADMIN');

-- Тестовый юзер (пароль: user123, bcrypt hash)
INSERT INTO users (email, password_hash, name, role)
VALUES ('user@treserve.com', '$2a$12$8KzaN8FaYk6yNFPLrE7PcOPmNfD1FzUqmhXqxGv.KPj/xLsDeOGMa', 'Test User', 'USER');

-- Площадки
INSERT INTO venues (name, address, rows_count, cols_count)
VALUES
    ('Октябрь — Зал 1', 'ул. Площадь Дружбы, д. 15', 5, 10),
    ('Лужники — Трибуна А', 'ул. Проспект Космонавтов, д. 24', 10, 20);

-- Места для "Октябрь — Зал 1" (5 рядов × 10 мест = 50)
INSERT INTO seats (venue_id, row_label, seat_number)
SELECT 1, chr(64 + r), s
FROM generate_series(1, 5) AS r,
     generate_series(1, 10) AS s;

-- Места для "Лужники — Трибуна А" (10 рядов × 20 мест = 200)
INSERT INTO seats (venue_id, row_label, seat_number)
SELECT 2, chr(64 + r), s
FROM generate_series(1, 10) AS r,
     generate_series(1, 20) AS s;

-- Мероприятия
INSERT INTO events (venue_id, title, description, image_url, age_restriction, category, duration_minutes, start_time, base_price, status, created_by)
VALUES
    (1, 'Inception — Ночной показ', 'Легендарный фильм Кристофера Нолана на большом экране', NULL, '12+', 'CINEMA', 148, NOW() + INTERVAL '7 days', 500.00, 'ACTIVE', 1),
    (1, 'Интерстеллар', 'Космическая одиссея', NULL, '12+', 'CINEMA', 169, NOW() + INTERVAL '14 days', 600.00, 'ACTIVE', 1),
    (2, 'Концерт Макса Коржа', 'Большой сольный концерт', NULL, '16+', 'CONCERT', 120, NOW() + INTERVAL '21 days', 1500.00, 'ACTIVE', 1);

-- Билеты для ивента 1 (Inception, venue 1 = 50 мест)
INSERT INTO tickets (event_id, seat_id, status, price)
SELECT 1, s.id, 'AVAILABLE', 500.00
FROM seats s WHERE s.venue_id = 1;

-- Билеты для ивента 2 (Интерстеллар, venue 1 = 50 мест)
INSERT INTO tickets (event_id, seat_id, status, price)
SELECT 2, s.id, 'AVAILABLE', 600.00
FROM seats s WHERE s.venue_id = 1;

-- Билеты для ивента 3 (Корж, venue 2 = 200 мест)
INSERT INTO tickets (event_id, seat_id, status, price)
SELECT 3, s.id, 'AVAILABLE', 1500.00
FROM seats s WHERE s.venue_id = 2;
