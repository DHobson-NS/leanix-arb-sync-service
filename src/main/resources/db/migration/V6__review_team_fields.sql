-- The Enterprise Architect and Solution Architect subscribed to a review (email addresses separated by ';') and the outcome of
-- subscribing them, taken from the review_team table through the review's manager.
ALTER TABLE architecture_review ADD COLUMN enterprise_architect VARCHAR(500);
ALTER TABLE architecture_review ADD COLUMN solution_architect VARCHAR(500);
ALTER TABLE architecture_review ADD COLUMN team_status VARCHAR(255);
