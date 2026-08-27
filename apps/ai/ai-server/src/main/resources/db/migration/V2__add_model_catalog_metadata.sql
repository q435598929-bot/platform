ALTER TABLE ai_model
  ADD COLUMN canonical_slug VARCHAR(200) NULL AFTER display_name,
  ADD COLUMN remote_created_at DATETIME(6) NULL AFTER canonical_slug,
  ADD COLUMN expiration_date VARCHAR(64) NULL AFTER remote_created_at,
  ADD COLUMN knowledge_cutoff VARCHAR(100) NULL AFTER expiration_date;
