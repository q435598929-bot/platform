ALTER TABLE platform_application
  ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER lifecycle_enabled;

CREATE INDEX idx_platform_application_sort
  ON platform_application(sort_order, id);
