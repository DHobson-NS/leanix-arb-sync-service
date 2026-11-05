-- A database that is new starts from now. V1 seeds the last run as 1970 so that the first run processes every existing
-- Initiative, and on a workspace where most Initiatives already have an Architecture Review that creates a new, unlinked
-- review for each of them (LeanIX allows one review per Initiative, so the link fails). Only a database still at the seed
-- value is changed; one that has run before keeps its last run.
UPDATE sync_state SET last_run_ts = now() WHERE last_run_ts = '1970-01-01 00:00:00';
