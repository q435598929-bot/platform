CREATE TABLE platform_application (
  id VARCHAR(100) NOT NULL,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(1000) NULL,
  category VARCHAR(50) NULL,
  source_root VARCHAR(1000) NOT NULL,
  runtime_type VARCHAR(50) NOT NULL,
  lifecycle_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE platform_component (
  id VARCHAR(100) NOT NULL,
  application_id VARCHAR(100) NOT NULL,
  name VARCHAR(150) NOT NULL,
  component_kind VARCHAR(30) NOT NULL,
  source_path VARCHAR(500) NOT NULL,
  technology VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_platform_component_app (application_id, sort_order),
  CONSTRAINT fk_platform_component_app FOREIGN KEY (application_id) REFERENCES platform_application(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE platform_application_link (
  id BIGINT NOT NULL AUTO_INCREMENT,
  application_id VARCHAR(100) NOT NULL,
  name VARCHAR(100) NOT NULL,
  url VARCHAR(1000) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_platform_link_app (application_id, sort_order),
  CONSTRAINT fk_platform_link_app FOREIGN KEY (application_id) REFERENCES platform_application(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
