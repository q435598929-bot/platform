CREATE TABLE ai_provider (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  api_key_ciphertext TEXT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_provider_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_model (
  id BIGINT NOT NULL AUTO_INCREMENT,
  provider_id BIGINT NOT NULL,
  code VARCHAR(150) NOT NULL,
  display_name VARCHAR(150) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  input_price_per_million DECIMAL(18,6) NOT NULL DEFAULT 0,
  output_price_per_million DECIMAL(18,6) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_model_provider_code (provider_id, code),
  CONSTRAINT fk_ai_model_provider FOREIGN KEY (provider_id) REFERENCES ai_provider(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_request_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  trace_id VARCHAR(36) NOT NULL,
  model_id BIGINT NOT NULL,
  success BOOLEAN NOT NULL,
  duration_ms BIGINT NOT NULL,
  input_tokens INT NOT NULL DEFAULT 0,
  output_tokens INT NOT NULL DEFAULT 0,
  estimated_cost DECIMAL(18,8) NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_request_trace (trace_id),
  KEY idx_ai_request_created (created_at),
  KEY idx_ai_request_model_created (model_id, created_at),
  CONSTRAINT fk_ai_request_model FOREIGN KEY (model_id) REFERENCES ai_model(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
