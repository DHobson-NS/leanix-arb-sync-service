-- Reduce review_team to one row per manager: the combination of Director, Manager and Enterprise Architect that occurs most often for the
-- manager in the inventory export (review_count adds up the fact sheets with that combination, whatever the Solution Architect was).
-- The Solution Architect kept on the row is the one that occurs most often within that combination. Rows without a manager cannot be
-- looked up and are dropped. On a tie the first Director, Enterprise Architect and Solution Architect in alphabetical order win.
CREATE TEMPORARY TABLE review_team_best AS
WITH combos AS (
    SELECT director, manager, enterprise_architect, SUM(review_count)::INTEGER AS review_count
    FROM review_team
    WHERE manager IS NOT NULL
    GROUP BY director, manager, enterprise_architect
),
best AS (
    SELECT DISTINCT ON (manager) director, manager, enterprise_architect, review_count
    FROM combos
    ORDER BY manager, review_count DESC, director, enterprise_architect
)
SELECT DISTINCT ON (b.manager) b.director, b.manager, b.enterprise_architect, t.solution_architect, b.review_count
FROM best b
JOIN review_team t ON t.manager = b.manager
                  AND t.director IS NOT DISTINCT FROM b.director
                  AND t.enterprise_architect IS NOT DISTINCT FROM b.enterprise_architect
ORDER BY b.manager, t.review_count DESC, t.solution_architect;

DROP TABLE review_team;

CREATE TABLE review_team (
    team_id              SERIAL       PRIMARY KEY,
    director             VARCHAR(500),
    manager              VARCHAR(500) NOT NULL UNIQUE,
    enterprise_architect VARCHAR(500),
    solution_architect   VARCHAR(500),
    review_count         INTEGER      NOT NULL DEFAULT 0
);

INSERT INTO review_team (director, manager, enterprise_architect, solution_architect, review_count)
SELECT director, manager, enterprise_architect, solution_architect, review_count FROM review_team_best ORDER BY manager;

DROP TABLE review_team_best;
