/**
 * ArbStatusResource.java
 *
 * Read-only status of arb-sync for the ARB page.
 * <p>
 * arb-sync keeps its tables in the arb schema of its own database; this reads the last run and the reviews it created.
 *
 * @author <a href="mailto:darrell.hobson@nscorp.com">Darrell L. Hobson</a>
 * @version 1.0.0
 * @since 1.0.0 (2026-10-29)
 *
 * @copyright Copyright 2026 Norfolk Southern Corporation (NS).
 * Title, ownership rights, and intellectual property
 * rights in and to this software remain with NS. NS hereby
 * reserves all rights in and to this software. This software
 * may not be copied or used without a license from NS. This
 * software is protected by international copyright laws and
 * treaties, and may be protected by other law. Violation of
 * copyright laws may result in civil liability and criminal
 * penalties.
 */
package com.nscorp.entarch.leanix.arb.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only status of arb-sync, under /api/status, for the ARB page of the admin site.
 * <p>
 * arb-sync keeps its tables in the arb schema of its own database. This reads when it last finished a
 * run (arb.sync_state), how many Architecture Reviews it has recorded and the latest ones (arb.architecture_review). If the
 * schema is not there yet, because arb-sync has not started, the answer says so instead of failing. Its schedule is not here:
 * it is configuration, the ARB_SYNC schedule of the configuration service.
 */
@Path("/api/status")
@Produces(MediaType.APPLICATION_JSON)
public class ArbStatusResource {

    /** Number of recent reviews returned. */
    static final int RECENT = 20;

    /** Database connections. */
    @Inject
    DataSource dataSource;

    /**
     * Returns the status of arb-sync.
     *
     * @return  A map with available (false if arb-sync has not created its tables), lastRun (when the last run finished, or
     *          null), reviews (how many reviews are recorded) and recent (the latest reviews with everything arb-sync
     *          sets on a review: initiativeName, initiativeId, reviewName, reviewId, recordId, phase, tag, manager,
     *          managerEmail, managerStatus, enterpriseArchitect, solutionArchitect,
     *          teamStatus, createdTs and createdBy).
     */
    @GET
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            result.put("lastRun", scalar(connection, "SELECT last_run_ts::text FROM arb.sync_state ORDER BY id LIMIT 1"));
            result.put("reviews", Long.parseLong(scalar(connection, "SELECT count(*)::text FROM arb.architecture_review")));
            List<Map<String, String>> recent = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT arb_name, initiative_name, created_ts::text, arb_id::text, initiative_id, arb_fs_id, arb_phase, arb_tag,"
                    + " it_manager, it_manager_email, manager_status, created_by, enterprise_architect, solution_architect, team_status"
                    + " FROM arb.architecture_review ORDER BY created_ts DESC LIMIT " + RECENT);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Map<String, String> review = new LinkedHashMap<>();
                    review.put("reviewName", rows.getString(1));
                    review.put("initiativeName", rows.getString(2));
                    review.put("createdTs", rows.getString(3));
                    review.put("recordId", rows.getString(4));
                    review.put("initiativeId", rows.getString(5));
                    review.put("reviewId", rows.getString(6));
                    review.put("phase", rows.getString(7));
                    review.put("tag", rows.getString(8));
                    review.put("manager", rows.getString(9));
                    review.put("managerEmail", rows.getString(10));
                    review.put("managerStatus", rows.getString(11));
                    review.put("createdBy", rows.getString(12));
                    review.put("enterpriseArchitect", rows.getString(13));
                    review.put("solutionArchitect", rows.getString(14));
                    review.put("teamStatus", rows.getString(15));
                    recent.add(review);
                }
            }
            result.put("recent", recent);
            result.put("available", true);
        } catch (SQLException ex) {
            result.clear();
            result.put("available", false);
            result.put("message", "the arb tables have not been created yet");
        }
        return result;
    }

    /**
     * Runs a query that returns one value.
     *
     * @param connection  The connection.
     * @param sql         The query.
     * @return             The first column of the first row as text, or null if there is no row.
     * @throws SQLException If the query fails, for example because the table does not exist.
     */
    private static String scalar(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getString(1) : null;
        }
    }
}
