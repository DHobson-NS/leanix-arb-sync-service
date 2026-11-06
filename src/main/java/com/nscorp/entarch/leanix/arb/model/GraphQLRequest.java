/**
 * GraphQLRequest.java
 *
 * Body of a request to the LeanIX GraphQL API.
 * <p>
 * Serialized to JSON with the properties "query" and "variables".
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

import java.util.Map;

/** A GraphQL query or mutation with its optional variables. */
public class GraphQLRequest {

    /** The GraphQL query or mutation text. */
    public String query;
    /** The values of the variables used in the query, or null if it has none. */
    public Map<String, Object> variables;

    /**
     * Creates a request without variables.
     *
     * @param query  The GraphQL query or mutation text.
     */
    public GraphQLRequest(String query) {
        this.query = query;
    }

    /**
     * Creates a request with variables.
     *
     * @param query      The GraphQL query or mutation text.
     * @param variables  The values of the variables used in the query.
     */
    public GraphQLRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }
}
