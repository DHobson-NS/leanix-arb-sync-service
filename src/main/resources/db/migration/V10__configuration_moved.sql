-- The review teams are configuration and now belong to the configuration service (leanix-sync-services-config), which this service
-- reads through its REST API. The schedule was never a table of this service.
DROP TABLE IF EXISTS review_team;
