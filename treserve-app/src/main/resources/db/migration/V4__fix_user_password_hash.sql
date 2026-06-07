-- Fix: correct bcrypt hash for user@treserve.com (password: user123)
UPDATE users
SET password_hash = '$2a$12$EDk4F/hZnsTzQ5f.O8STcu0pDJWw5rEXPy6hRrcz3lxuq.SbNVoey'
WHERE email = 'user@treserve.com';

-- Fix: add image URLs to seed events (were NULL)
UPDATE events SET image_url = 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800' WHERE id = 1;
UPDATE events SET image_url = 'https://images.unsplash.com/photo-1534796636912-3b95b3ab5986?w=800' WHERE id = 2;
UPDATE events SET image_url = 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800' WHERE id = 3;

DELETE FROM users WHERE email IN ('fixhash@temp.com', 'u123@temp.com', 'hash@temp.com');

