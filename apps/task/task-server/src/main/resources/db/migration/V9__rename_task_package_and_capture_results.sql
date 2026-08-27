ALTER TABLE task_execution ADD COLUMN result_json TEXT NULL AFTER context_json;
UPDATE task_execution SET result_json = '{}' WHERE result_json IS NULL;
ALTER TABLE task_execution MODIFY result_json TEXT NOT NULL;

UPDATE task_template
SET class_name = REPLACE(class_name, 'com.lab.task.', 'com.platform.task.')
WHERE class_name LIKE 'com.lab.task.%';

UPDATE task_definition
SET class_name = REPLACE(class_name, 'com.lab.task.', 'com.platform.task.')
WHERE class_name LIKE 'com.lab.task.%';
