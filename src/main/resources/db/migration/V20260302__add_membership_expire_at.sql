ALTER TABLE users
ADD COLUMN membership_expire_at DATETIME NULL COMMENT '会员到期时间（NULL 表示永久）' AFTER membership_id;
