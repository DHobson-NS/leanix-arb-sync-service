-- The name of the Initiative the review was created for, so the ARB page can show names instead of ids. Rows created before this
-- column existed are filled in from LeanIX by arb-sync (at startup and on each run).
ALTER TABLE architecture_review ADD COLUMN initiative_name VARCHAR(255);
