-- Rebuild every job identity with the same normalized company|title|city contract used by Java.
-- Legacy logical duplicates are preserved: the oldest row keeps the canonical hash and later
-- rows receive a deterministic legacy hash so user-job references are never deleted implicitly.
ALTER TABLE jobs DROP INDEX uk_unique_hash;

UPDATE jobs
SET unique_hash = MD5(CONCAT(
        LOWER(TRIM(company_name)), '|',
        LOWER(TRIM(job_title)), '|',
        LOWER(TRIM(city))));

CREATE TEMPORARY TABLE job_hash_ranks
SELECT id,
       unique_hash,
       ROW_NUMBER() OVER (PARTITION BY unique_hash ORDER BY id) AS duplicate_rank
FROM jobs;

UPDATE jobs AS job
    INNER JOIN job_hash_ranks AS ranked ON ranked.id = job.id
SET job.unique_hash = MD5(CONCAT(ranked.unique_hash, '|legacy|', job.id))
WHERE ranked.duplicate_rank > 1;

DROP TEMPORARY TABLE job_hash_ranks;

ALTER TABLE jobs
    MODIFY COLUMN unique_hash CHAR(32) NOT NULL COMMENT '规范化公司名|职位名|城市的 MD5',
    ADD CONSTRAINT uk_unique_hash UNIQUE (unique_hash);

-- Insert only missing required membership IDs; preserve operator-customized existing rows.
INSERT INTO memberships (id, level_name, price, duration_days, privileges, icon_url)
SELECT 1, '普通用户', 0.00, -1, JSON_OBJECT('max_job_track', 5), NULL
WHERE NOT EXISTS (SELECT 1 FROM memberships WHERE id = 1);

INSERT INTO memberships (id, level_name, price, duration_days, privileges, icon_url)
SELECT 2, 'VIP会员', 0.00, 30, JSON_OBJECT('max_job_track', -1), NULL
WHERE NOT EXISTS (SELECT 1 FROM memberships WHERE id = 2);
