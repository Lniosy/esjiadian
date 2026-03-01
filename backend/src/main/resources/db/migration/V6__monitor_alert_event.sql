CREATE TABLE IF NOT EXISTS monitor_alert_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_key VARCHAR(64) NOT NULL,
  level VARCHAR(16) NOT NULL,
  title VARCHAR(120) NOT NULL,
  message VARCHAR(500) NOT NULL,
  current_value DOUBLE,
  threshold_value DOUBLE,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  ack_by BIGINT NULL,
  ack_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  INDEX idx_monitor_alert_key (alert_key, status),
  INDEX idx_monitor_alert_status (status, created_at)
);
