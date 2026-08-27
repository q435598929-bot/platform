CREATE TABLE task_workflow_template (
  id VARCHAR(120) NOT NULL,
  display_name VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  category VARCHAR(80) NOT NULL,
  version INT NOT NULL,
  steps_json TEXT NOT NULL,
  dangerous BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE task_definition
  ADD COLUMN workflow_template_id VARCHAR(120) NULL AFTER template_task_id,
  ADD KEY idx_task_definition_workflow (workflow_template_id),
  ADD CONSTRAINT fk_task_definition_workflow
    FOREIGN KEY (workflow_template_id) REFERENCES task_workflow_template(id);

ALTER TABLE task_execution
  ADD COLUMN execution_type VARCHAR(30) NOT NULL DEFAULT 'LEGACY' AFTER inputs_json,
  ADD COLUMN current_step_index INT NOT NULL DEFAULT 0 AFTER execution_type,
  ADD COLUMN context_json TEXT NULL AFTER current_step_index,
  ADD COLUMN next_run_at DATETIME(6) NULL AFTER context_json,
  ADD KEY idx_task_execution_waiting (status, next_run_at);

UPDATE task_execution SET context_json = '{}' WHERE context_json IS NULL;
ALTER TABLE task_execution MODIFY context_json TEXT NOT NULL;

CREATE TABLE task_step_execution (
  id VARCHAR(36) NOT NULL,
  execution_id VARCHAR(36) NOT NULL,
  step_index INT NOT NULL,
  step_key VARCHAR(120) NOT NULL,
  template_task_id VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  inputs_json TEXT NOT NULL,
  outputs_json TEXT NOT NULL,
  started_at DATETIME(6) NULL,
  finished_at DATETIME(6) NULL,
  next_run_at DATETIME(6) NULL,
  error_message VARCHAR(2000) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_step_execution (execution_id, step_index),
  KEY idx_task_step_status_time (status, next_run_at),
  CONSTRAINT fk_task_step_execution
    FOREIGN KEY (execution_id) REFERENCES task_execution(id) ON DELETE CASCADE,
  CONSTRAINT fk_task_step_template
    FOREIGN KEY (template_task_id) REFERENCES task_template(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO task_template
  (id, display_name, description, category, class_name, dangerous, created_at, updated_at)
VALUES
  ('merchant-basicdata-modify', '商户基本信息修改', '调用汇付商户基本信息修改并返回申请单信息。', '商户管理',
   'com.lab.task.controller.onboarding.MerchantBasicdataModifyTask', TRUE, NOW(6), NOW(6)),
  ('merchant-business-modify', '商户业务开通修改', '调用汇付商户业务开通修改并返回申请单信息。', '商户管理',
   'com.lab.task.controller.onboarding.MerchantBusinessModifyTask', TRUE, NOW(6), NOW(6));

INSERT INTO task_workflow_template
  (id, display_name, description, category, version, steps_json, dangerous, created_at, updated_at)
VALUES
  ('merchant-basicdata-modify-query', '商户基本信息修改 + 申请单查询',
   '可选上传图片，提交商户基本信息修改，并按间隔查询申请单；处理中可自动轮询或手动立即查询。',
   '商户管理流程', 1,
   '[{"key":"picture-upload","templateTaskId":"merchant-picture-upload","optional":true,"polling":false,"intervalSeconds":0,"maxAttempts":1},{"key":"basicdata-modify","templateTaskId":"merchant-basicdata-modify","optional":false,"polling":false,"intervalSeconds":0,"maxAttempts":1},{"key":"application-query","templateTaskId":"merchant-application-status-query","optional":false,"polling":true,"intervalSeconds":30,"maxAttempts":120}]',
   TRUE, NOW(6), NOW(6)),
  ('merchant-business-open-query', '商户业务开通 + 申请单查询',
   '可选上传图片，提交商户业务开通，并按间隔查询申请单；处理中可自动轮询或手动立即查询。',
   '商户管理流程', 1,
   '[{"key":"picture-upload","templateTaskId":"merchant-picture-upload","optional":true,"polling":false,"intervalSeconds":0,"maxAttempts":1},{"key":"business-open","templateTaskId":"merchant-business-open","optional":false,"polling":false,"intervalSeconds":0,"maxAttempts":1},{"key":"application-query","templateTaskId":"merchant-application-status-query","optional":false,"polling":true,"intervalSeconds":30,"maxAttempts":120}]',
   TRUE, NOW(6), NOW(6)),
  ('merchant-business-modify-query', '商户业务开通修改 + 申请单查询',
   '可选上传图片，提交商户业务开通修改，并按间隔查询申请单；处理中可自动轮询或手动立即查询。',
   '商户管理流程', 1,
   '[{"key":"picture-upload","templateTaskId":"merchant-picture-upload","optional":true,"polling":false,"intervalSeconds":0,"maxAttempts":1},{"key":"business-modify","templateTaskId":"merchant-business-modify","optional":false,"polling":false,"intervalSeconds":0,"maxAttempts":1},{"key":"application-query","templateTaskId":"merchant-application-status-query","optional":false,"polling":true,"intervalSeconds":30,"maxAttempts":120}]',
   TRUE, NOW(6), NOW(6));
