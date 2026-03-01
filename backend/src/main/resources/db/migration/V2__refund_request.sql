CREATE TABLE IF NOT EXISTS refund_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reject_reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_refund_order (order_id),
  INDEX idx_refund_applicant (applicant_id, created_at)
);
