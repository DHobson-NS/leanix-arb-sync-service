-- What arb-sync sets on a new ArchitectureReview, so the ARB page can show all of it: the phase and tag (always Open at creation),
-- the Initiative's IT manager, and the outcome of assigning that manager as a subscriber. Rows from before this column existed had
-- phase and tag set to Open; their manager is not recorded.
ALTER TABLE architecture_review ADD COLUMN arb_phase VARCHAR(30);
ALTER TABLE architecture_review ADD COLUMN arb_tag VARCHAR(60);
ALTER TABLE architecture_review ADD COLUMN it_manager VARCHAR(255);
ALTER TABLE architecture_review ADD COLUMN it_manager_email VARCHAR(255);
ALTER TABLE architecture_review ADD COLUMN manager_status VARCHAR(255);
UPDATE architecture_review SET arb_phase = 'Open', arb_tag = 'Open';
