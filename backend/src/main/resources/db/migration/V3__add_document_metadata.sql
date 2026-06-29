-- Migration: Add Document Intelligence metadata extraction columns
ALTER TABLE documents ADD COLUMN extracted_tags TEXT;
ALTER TABLE documents ADD COLUMN extracted_failure_modes TEXT;
ALTER TABLE documents ADD COLUMN extracted_procedures TEXT;
ALTER TABLE documents ADD COLUMN extracted_safety_references TEXT;
ALTER TABLE documents ADD COLUMN extracted_work_orders TEXT;
ALTER TABLE documents ADD COLUMN extracted_findings TEXT;
