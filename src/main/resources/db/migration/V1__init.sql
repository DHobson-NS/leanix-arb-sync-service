-- Architecture Review records created by the ARB sync process
CREATE TABLE IF NOT EXISTS architecture_review (
    ARB_ID      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    arb_name    VARCHAR(36)  NOT NULL,
    created_ts  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(24)  NOT NULL DEFAULT 'leanIX_arb_upd',
    -- Additional columns for deduplication and traceability (not in base spec)
    initiative_id  VARCHAR(36) UNIQUE,   -- LeanIX Initiative fact sheet ID
    arb_fs_id      VARCHAR(36)           -- LeanIX ArchitectureReview fact sheet ID
);

-- Single-row table that tracks when the scheduler last successfully completed
CREATE TABLE IF NOT EXISTS sync_state (
    id          SERIAL       PRIMARY KEY,
    last_run_ts TIMESTAMP    NOT NULL DEFAULT '1970-01-01 00:00:00'
);

-- Seed the initial sync state so the first run processes all existing Projects
INSERT INTO sync_state (last_run_ts) VALUES ('1970-01-01 00:00:00');
