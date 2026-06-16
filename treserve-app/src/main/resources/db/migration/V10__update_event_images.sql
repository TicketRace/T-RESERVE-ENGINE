-- V10__update_event_images.sql — Переключение на локальные картинки
UPDATE events SET image_url = '/event%20cards/inception.jpg' WHERE title = 'Inception — Ночной показ';
UPDATE events SET image_url = '/event%20cards/interstlerral.jpg' WHERE title = 'Интерстеллар';
UPDATE events SET image_url = '/event%20cards/corzha.jpg' WHERE title = 'Концерт Макса Коржа';
UPDATE events SET image_url = '/event%20cards/annacar.avif' WHERE title = 'Анна Каренина';
UPDATE events SET image_url = '/event%20cards/mciry.jpg' WHERE title = 'Мцыри';
