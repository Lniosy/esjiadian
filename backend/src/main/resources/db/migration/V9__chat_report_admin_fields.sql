ALTER TABLE chat_message_report
  ADD COLUMN reviewed_by BIGINT NULL AFTER status,
  ADD COLUMN decision_note VARCHAR(255) NULL AFTER reviewed_by,
  ADD COLUMN reviewed_at DATETIME NULL AFTER decision_note;

CREATE INDEX idx_chat_report_status_time ON chat_message_report(status, created_at);
