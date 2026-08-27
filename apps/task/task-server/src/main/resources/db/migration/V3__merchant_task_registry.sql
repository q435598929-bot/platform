CREATE TABLE task_merchant_profile (
  id VARCHAR(36) NOT NULL,
  code VARCHAR(80) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  configuration_json TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_merchant_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE task_template (
  id VARCHAR(120) NOT NULL,
  display_name VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  category VARCHAR(80) NOT NULL,
  class_name VARCHAR(500) NOT NULL,
  dangerous BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_template_class (class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO task_template (id, display_name, description, category, class_name, dangerous, created_at, updated_at)
SELECT id, display_name, description, category, class_name, dangerous, created_at, updated_at
FROM task_definition;

ALTER TABLE task_definition
  DROP INDEX uk_task_definition_class,
  ADD COLUMN merchant_id VARCHAR(36) NULL AFTER category,
  ADD COLUMN template_task_id VARCHAR(120) NULL AFTER merchant_id,
  ADD KEY idx_task_definition_merchant (merchant_id),
  ADD KEY idx_task_definition_template (template_task_id),
  ADD CONSTRAINT fk_task_definition_merchant FOREIGN KEY (merchant_id) REFERENCES task_merchant_profile(id),
  ADD CONSTRAINT fk_task_definition_template FOREIGN KEY (template_task_id) REFERENCES task_template(id);

INSERT INTO task_merchant_profile (id, code, name, description, configuration_json, created_at, updated_at)
SELECT LOWER(category), category, category, '由现有任务分类迁移生成', '{}', NOW(6), NOW(6)
FROM task_definition
GROUP BY category;

UPDATE task_definition
SET merchant_id = LOWER(category), template_task_id = id;
