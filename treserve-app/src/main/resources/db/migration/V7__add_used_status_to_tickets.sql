-- Удаляем старый check constraint
ALTER TABLE tickets DROP CONSTRAINT tickets_status_check;

-- Создаём новый с добавлением статуса USED
ALTER TABLE tickets ADD CONSTRAINT tickets_status_check 
CHECK (status IN ('AVAILABLE', 'LOCKED', 'BOOKED', 'USED'));