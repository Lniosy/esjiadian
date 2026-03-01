ALTER TABLE refund_request
  ADD COLUMN return_company_code VARCHAR(32) NULL AFTER reject_reason,
  ADD COLUMN return_tracking_no VARCHAR(64) NULL AFTER return_company_code,
  ADD COLUMN return_ship_note VARCHAR(255) NULL AFTER return_tracking_no,
  ADD COLUMN return_shipped_at DATETIME NULL AFTER return_ship_note,
  ADD COLUMN return_received_at DATETIME NULL AFTER return_shipped_at;
