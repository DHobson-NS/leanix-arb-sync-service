/**
 * A review team: the people that normally review the projects of one manager.
 * <p>
 * Stored in the table review_team of the arb schema, loaded from a LeanIX inventory export of ArchitectureReview fact sheets: one row for
 * each manager (the Director, Enterprise Architect and Solution Architect that occur most often for that manager). The rows are edited on
 * the team configuration page of the cmdb-sync web site.
 *
 * @author <a href="mailto:darrell.hobson@nscorp.com">Darrell L. Hobson</a>
 * @version 1.0.0
 * @since 2026-10-30
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
package com.nscorp.entarch.leanix.arb.entity;

/**
 * One row of the review teams table of the configuration service: the Director, Enterprise Architect and Solution Architect that
 * are subscribed to the Architecture Review of a manager. Email cells with several people separate them with ';'. The fields have the
 * names of the JSON of the review team API.
 */
public class ReviewTeam {

    /** Id of the row. */
    public Integer id;

    /** Director email address(es). */
    public String director;

    /** First name(s) of the Director. */
    public String directorFirstName;

    /** Last name(s) of the Director. */
    public String directorLastName;

    /** Manager email address; one row for each manager. */
    public String manager;

    /** First name of the Manager. */
    public String managerFirstName;

    /** Last name of the Manager. */
    public String managerLastName;

    /** Enterprise Architect email address(es). */
    public String enterpriseArchitect;

    /** First name(s) of the Enterprise Architect. */
    public String enterpriseArchitectFirstName;

    /** Last name(s) of the Enterprise Architect. */
    public String enterpriseArchitectLastName;

    /** Solution Architect email address(es). */
    public String solutionArchitect;

    /** First name(s) of the Solution Architect. */
    public String solutionArchitectFirstName;

    /** Last name(s) of the Solution Architect. */
    public String solutionArchitectLastName;

    /** How many fact sheets in the inventory export had this combination; the row with most wins for a manager. */
    public Integer reviewCount;
}
