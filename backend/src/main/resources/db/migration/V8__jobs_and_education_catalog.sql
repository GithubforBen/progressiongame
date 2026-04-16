-- V8: Expanded job catalog – add category, max_parallel, required_side_cert columns

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS category          VARCHAR(50)  NOT NULL DEFAULT 'EINSTIEG',
    ADD COLUMN IF NOT EXISTS max_parallel       INT          NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS required_side_cert VARCHAR(100);

-- Mark all existing jobs unavailable; GameDataLoaderService re-enables every job in jobs.yaml
UPDATE jobs SET available = false;
