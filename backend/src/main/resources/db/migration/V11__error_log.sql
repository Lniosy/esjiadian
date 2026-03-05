CREATE TABLE IF NOT EXISTS error_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  path VARCHAR(255) NOT NULL,
  method VARCHAR(16) NOT NULL,
  http_status INT NOT NULL,
  error_code INT NULL,
  error_message VARCHAR(500) NOT NULL,
  exception_class VARCHAR(255) NULL,
  stack_trace TEXT NULL,
  client_ip VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  INDEX idx_error_log_time (created_at),
  INDEX idx_error_log_status_time (http_status, created_at),
  INDEX idx_error_log_user_time (user_id, created_at)
);
