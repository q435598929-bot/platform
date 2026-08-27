ALTER TABLE task_execution
  ADD COLUMN inputs_json TEXT NULL AFTER arguments_json;

UPDATE task_execution SET inputs_json = '{}' WHERE inputs_json IS NULL;

ALTER TABLE task_execution MODIFY inputs_json TEXT NOT NULL;
