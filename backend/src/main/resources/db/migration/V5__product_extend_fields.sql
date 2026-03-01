ALTER TABLE product
  ADD COLUMN purchase_date DATE NULL AFTER model,
  ADD COLUMN repair_history VARCHAR(500) NULL AFTER function_status,
  ADD COLUMN video_url VARCHAR(255) NULL AFTER description,
  ADD COLUMN sales_count INT NOT NULL DEFAULT 0 AFTER sold;
