-- ══════════════════════════════════════════════
-- V3__add_mock_events.sql — Добавление мок-данных из песочницы
-- ══════════════════════════════════════════════

-- Исправляем старые категории на русские (как ожидает фронтенд)
UPDATE events SET category = 'Концерт' WHERE category = 'CONCERT';
UPDATE events SET category = 'Кино' WHERE category = 'CINEMA';

-- Добавляем события из песочницы (Анна Каренина и Мцыри)
INSERT INTO events (venue_id, title, description, image_url, age_restriction, category, duration_minutes, start_time, base_price, status, created_by)
VALUES
    (1, 'Анна Каренина', 'Камерная постановка классического романа на большой сцене.', 'https://images.unsplash.com/photo-1514306191717-452ec28c7814?auto=format&fit=crop&q=80&w=1200', '16+', 'Спектакль', 120, NOW() + INTERVAL '5 days', 800.00, 'ACTIVE', 1),
    (1, 'Мцыри', 'Пластический спектакль о свободе и выборе.', 'https://images.unsplash.com/photo-1478147424095-2dd8b839ec9f?auto=format&fit=crop&q=80&w=1200', '12+', 'Спектакль', 90, NOW() + INTERVAL '6 days', 700.00, 'ACTIVE', 1);

-- Генерируем билеты для 'Анна Каренина' (venue 1 = 50 мест).
INSERT INTO tickets (event_id, seat_id, status, price)
SELECT (SELECT id FROM events WHERE title = 'Анна Каренина' LIMIT 1), s.id, 'AVAILABLE', 800.00
FROM seats s WHERE s.venue_id = 1;

-- Генерируем билеты для 'Мцыри' (venue 1 = 50 мест).
INSERT INTO tickets (event_id, seat_id, status, price)
SELECT (SELECT id FROM events WHERE title = 'Мцыри' LIMIT 1), s.id, 'AVAILABLE', 700.00
FROM seats s WHERE s.venue_id = 1;
