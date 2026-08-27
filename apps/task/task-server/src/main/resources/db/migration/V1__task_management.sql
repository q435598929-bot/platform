CREATE TABLE task_definition (
  id VARCHAR(120) NOT NULL,
  display_name VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  category VARCHAR(80) NOT NULL,
  class_name VARCHAR(500) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  dangerous BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_definition_class (class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE task_execution (
  id VARCHAR(36) NOT NULL,
  task_id VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL,
  arguments_json TEXT NOT NULL,
  trigger_source VARCHAR(50) NOT NULL,
  confirmed BOOLEAN NOT NULL,
  requested_at DATETIME(6) NOT NULL,
  started_at DATETIME(6) NULL,
  finished_at DATETIME(6) NULL,
  error_message VARCHAR(2000) NULL,
  PRIMARY KEY (id),
  KEY idx_task_execution_task_time (task_id, requested_at),
  KEY idx_task_execution_status_time (status, requested_at),
  CONSTRAINT fk_task_execution_definition FOREIGN KEY (task_id) REFERENCES task_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE task_execution_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  execution_id VARCHAR(36) NOT NULL,
  level VARCHAR(20) NOT NULL,
  message TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_task_log_execution (execution_id, id),
  CONSTRAINT fk_task_log_execution FOREIGN KEY (execution_id) REFERENCES task_execution(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
