/**
 * FactSheetNode.java
 *
 * A LeanIX fact sheet as returned by the GraphQL API, with the wrapper types of the responses.
 * <p>
 * Mirrors the nested response shape of allFactSheets, createFactSheet and updateFactSheet.
 * Unknown properties are ignored.
 *
 * @author <a href="mailto:darrell.hobson@nscorp.com">Darrell L. Hobson</a>
 * @version 1.0.0
 * @since 1.0.0 (2026-10-30)
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
package com.nscorp.entarch.leanix.arb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors the nested GraphQL response shape returned by allFactSheets /
 * createFactSheet / updateFactSheet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FactSheetNode {

    /** LeanIX fact sheet id. */
    public String id;
    /** Name of the fact sheet. */
    public String name;
    /** Name as LeanIX shows it, including any hierarchy prefix. */
    public String displayName;
    /** Creation time as an ISO 8601 timestamp. */
    public String createdAt;

    /** The itDirector attribute of an Initiative: the IT director's name as "Last; First", or null. */
    public String itDirector;

    /** The itManager attribute of an Initiative: the IT manager's name, or null. */
    public String itManager;

    /** The itManagerEmail attribute of an Initiative: the IT manager's email address, or null. */
    public String itManagerEmail;

    // ── allFactSheets wrapper types ──────────────────────────────────────────

    /** One entry of the allFactSheets result list. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Edge {
        /** The fact sheet of this entry. */
        public FactSheetNode node;
    }

    /** The list of entries of the allFactSheets result. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Connection {
        /** The entries of the result. */
        public List<Edge> edges;
    }

    /** The data element of an allFactSheets response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AllFactSheetsData {
        /** The result of the allFactSheets query. */
        public Connection allFactSheets;
    }

    /** A complete allFactSheets response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AllFactSheetsResponse {
        /** The data element of the response. */
        public AllFactSheetsData data;
        /** The errors LeanIX reported, or null if there were none. */
        public List<GraphQLError> errors;
    }

    // ── createFactSheet wrapper types ────────────────────────────────────────

    /** The result of the createFactSheet mutation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateResult {
        /** The fact sheet that was created. */
        public FactSheetNode factSheet;
    }

    /** The data element of a createFactSheet response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateData {
        /** The result of the createFactSheet mutation. */
        public CreateResult createFactSheet;
    }

    /** A complete createFactSheet response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateResponse {
        /** The data element of the response. */
        public CreateData data;
        public List<GraphQLError> errors;
    }

    // ── updateFactSheet wrapper types ────────────────────────────────────────

    /** The result of the updateFactSheet mutation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateResult {
        public FactSheetNode factSheet;
    }

    /** The data element of an updateFactSheet response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateData {
        /** The result of the updateFactSheet mutation. */
        public UpdateResult updateFactSheet;
    }

    /** A complete updateFactSheet response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateResponse {
        /** The data element of the response. */
        public UpdateData data;
        public List<GraphQLError> errors;
    }

    // ── Error ────────────────────────────────────────────────────────────────

    /** One error reported by the GraphQL API. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphQLError {
        /** Description of the error. */
        public String message;
    }
}
