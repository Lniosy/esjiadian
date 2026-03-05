CREATE TABLE IF NOT EXISTS user_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_favorite_user_product (user_id, product_id),
  INDEX idx_favorite_user_time (user_id, created_at),
  INDEX idx_favorite_product_time (product_id, created_at)
);
