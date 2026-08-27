CREATE TABLE ai_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  model_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ai_conversation_model_updated (model_id, updated_at),
  KEY idx_ai_conversation_created (created_at),
  CONSTRAINT fk_ai_conversation_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_conversation_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(30) NOT NULL,
  content TEXT NOT NULL,
  trace_id VARCHAR(36) NULL,
  input_tokens INT NOT NULL DEFAULT 0,
  output_tokens INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ai_conversation_message_order (conversation_id, created_at, id),
  CONSTRAINT fk_ai_conversation_message FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
