-- Migration: Add Google OAuth fields and relax password_hash constraint
ALTER TABLE users ADD COLUMN display_name VARCHAR(255);
ALTER TABLE users ADD COLUMN profile_picture TEXT;
ALTER TABLE users ADD COLUMN provider VARCHAR(50) DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN google_subject_id VARCHAR(255) UNIQUE;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
