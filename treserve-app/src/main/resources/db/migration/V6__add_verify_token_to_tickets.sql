-- Добавляем колонку verify_token в таблицу tickets
ALTER TABLE tickets ADD COLUMN verify_token UUID UNIQUE DEFAULT NULL;

-- Создаем индекс для быстрого поиска по токену
CREATE INDEX idx_tickets_verify_token ON tickets(verify_token);