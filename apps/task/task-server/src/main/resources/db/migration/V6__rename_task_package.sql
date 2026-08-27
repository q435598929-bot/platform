UPDATE task_template
SET class_name = REPLACE(class_name, 'com.lab.taskexecutor.', 'com.lab.task.')
WHERE class_name LIKE 'com.lab.taskexecutor.%';

UPDATE task_definition
SET class_name = REPLACE(class_name, 'com.lab.taskexecutor.', 'com.lab.task.')
WHERE class_name LIKE 'com.lab.taskexecutor.%';
