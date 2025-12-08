ALTER TABLE plans
ALTER COLUMN included_credits TYPE BIGINT USING included_credits::bigint;