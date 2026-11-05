-- First and last name of the Director and the Manager of each review team, filled from the Initiatives.
ALTER TABLE arb.review_team ADD COLUMN director_first_name VARCHAR(100);
ALTER TABLE arb.review_team ADD COLUMN director_last_name  VARCHAR(100);
ALTER TABLE arb.review_team ADD COLUMN manager_first_name  VARCHAR(100);
ALTER TABLE arb.review_team ADD COLUMN manager_last_name   VARCHAR(100);
