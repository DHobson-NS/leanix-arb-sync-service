-- First and last names of the Enterprise Architects and Solution Architects; several people are separated by ';' in the order of the addresses.
ALTER TABLE arb.review_team ADD COLUMN enterprise_architect_first_name VARCHAR(500);
ALTER TABLE arb.review_team ADD COLUMN enterprise_architect_last_name  VARCHAR(500);
ALTER TABLE arb.review_team ADD COLUMN solution_architect_first_name   VARCHAR(500);
ALTER TABLE arb.review_team ADD COLUMN solution_architect_last_name    VARCHAR(500);
