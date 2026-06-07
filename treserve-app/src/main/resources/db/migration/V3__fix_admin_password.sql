-- V3__fix_admin_password.sql
-- Fix: incorrect bcrypt hash for admin123 in seed
UPDATE users
SET password_hash = '$2a$12$kiJYP0.Vz5kxkqe5qc.R1O2lrdaIHQV7.ZHADZygHPQeofRsh1yEC'
WHERE email = 'admin@treserve.com';
