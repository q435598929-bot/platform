CREATE TABLE platform_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(150) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  token_version INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE platform_role (
  id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE platform_permission (
  id BIGINT NOT NULL,
  code VARCHAR(100) NOT NULL,
  name VARCHAR(150) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE platform_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_platform_user_role_user FOREIGN KEY (user_id) REFERENCES platform_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_platform_user_role_role FOREIGN KEY (role_id) REFERENCES platform_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE platform_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_platform_role_permission_role FOREIGN KEY (role_id) REFERENCES platform_role(id) ON DELETE CASCADE,
  CONSTRAINT fk_platform_role_permission_permission FOREIGN KEY (permission_id) REFERENCES platform_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE platform_revoked_token (
  jti VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NOT NULL,
  PRIMARY KEY (jti),
  KEY idx_platform_revoked_token_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO platform_role (id, code, name) VALUES
  (1, 'ADMIN', '管理员'), (2, 'OPERATOR', '操作员'), (3, 'VIEWER', '只读用户');

INSERT INTO platform_permission (id, code, name) VALUES
  (1, 'platform:read', '查看平台应用'),
  (2, 'platform:lifecycle', '启动停止应用'),
  (3, 'platform:admin', '管理用户与注册表'),
  (4, 'ai:read', '查看 AI 配置与记录'),
  (5, 'ai:write', '维护 AI 服务商与模型'),
  (6, 'ai:execute', '发起 AI 对话'),
  (7, 'task:read', '查看任务与执行记录'),
  (8, 'task:manage', '启用禁用任务'),
  (9, 'task:execute', '执行任务');

INSERT INTO platform_role_permission (role_id, permission_id)
SELECT 1, id FROM platform_permission;
INSERT INTO platform_role_permission (role_id, permission_id) VALUES
  (2,1),(2,2),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),
  (3,1),(3,4),(3,7);
