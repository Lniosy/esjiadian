-- Demo seed script (idempotent)
-- Usage:
--   mysql -uroot -proot used_appliance < backend/src/main/resources/db/seed/seed_demo.sql

INSERT INTO category (id, parent_id, name, sort)
VALUES
  (1, NULL, '大家电', 1),
  (2, NULL, '小家电', 2),
  (3, NULL, '厨电', 3),
  (4, NULL, '数码', 4)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  sort = VALUES(sort);

-- Admin account is ensured by DataBootstrap on app startup:
--   admin@example.com / Admin1234
