ALTER TABLE recommend_trace
  ADD COLUMN category_id BIGINT NULL AFTER product_id;

ALTER TABLE recommend_trace
  ADD INDEX idx_rec_category_time (category_id, created_at);
