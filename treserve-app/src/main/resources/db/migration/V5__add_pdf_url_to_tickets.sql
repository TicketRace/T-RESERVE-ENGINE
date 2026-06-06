-- Add pdf_url column to tickets table for storing PDF path in MinIO/S3

ALTER TABLE tickets ADD COLUMN pdf_url VARCHAR(512) DEFAULT NULL;