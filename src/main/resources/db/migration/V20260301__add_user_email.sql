ALTER TABLE users
    ADD COLUMN email VARCHAR(100) NULL COMMENT 'independent email for code-login and password reset' AFTER username;

ALTER TABLE users
    ADD UNIQUE INDEX uk_email (email);
