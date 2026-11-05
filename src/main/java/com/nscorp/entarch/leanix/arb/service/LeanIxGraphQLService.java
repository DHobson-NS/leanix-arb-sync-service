/**
 * Reads and writes LeanIX fact sheets through the GraphQL API.
 * <p>
 * Holds the queries and mutations used by the ARB sync: reading the Initiatives, creating an ArchitectureReview,
 * linking it to an Initiative, and setting its phase and tag.
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
package com.nscorp.entarch.leanix.arb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nscorp.entarch.leanix.arb.client.LeanIxGraphQLClient;
import com.nscorp.entarch.leanix.arb.config.LeanIxConfig;
import com.nscorp.entarch.leanix.arb.model.FactSheetNode;
import com.nscorp.entarch.leanix.arb.model.GraphQLRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** LeanIX GraphQL operations used by the ARB sync. */
@ApplicationScoped
public class LeanIxGraphQLService {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(LeanIxGraphQLService.class);

    /** Query that reads all Initiative fact sheets. */
    private static final String QUERY_INITIATIVES = """
            {
              allFactSheets(
                filter: {
                  facetFilters: [
                    { facetKey: "FactSheetTypes", operator: OR, keys: ["Initiative"] }
                  ]
                }
              ) {
                edges {
                  node {
                    id
                    name
                    displayName
                    createdAt
                    ... on Initiative {
                      itDirector
                      itManager
                      itManagerEmail
                    }
                  }
                }
              }
            }
            """;

    /** Name of the relation from an ArchitectureReview to its Initiative. */
    private static final String RELATION_TO_INITIATIVE = "relArchitectureReviewToInitiative";

    /** Query that reads one page of ArchitectureReview fact sheets with the Initiatives they are linked to. */
    private static final String QUERY_REVIEW_LINKS = """
            query($after: String) {
              allFactSheets(
                first: 500
                after: $after
                filter: {
                  facetFilters: [
                    { facetKey: "FactSheetTypes", operator: OR, keys: ["ArchitectureReview"] }
                  ]
                }
              ) {
                pageInfo { hasNextPage endCursor }
                edges {
                  node {
                    id
                    ... on ArchitectureReview {
                      relArchitectureReviewToInitiative {
                        edges { node { factSheet { id } } }
                      }
                    }
                  }
                }
              }
            }
            """;

    /** Query that reads the subscription roles of the workspace. */
    private static final String QUERY_ROLES = """
            { allSubscriptionRoles { edges { node { id name } } } }
            """;

    /** Mutation that subscribes a user to a fact sheet with a role. */
    private static final String MUTATION_CREATE_SUBSCRIPTION = """
            mutation AddSubscription($factSheetId: ID!, $user: UserInput!, $type: FactSheetSubscriptionType!,
                                     $roles: [SubscriptionToSubscriptionRoleLinkInput]) {
              createSubscription(factSheetId: $factSheetId, user: $user, type: $type, roles: $roles) {
                id
              }
            }
            """;

    /** Mutation that creates a fact sheet. */
    private static final String MUTATION_CREATE_ARB = """
            mutation CreateArchitectureReview($input: BaseFactSheetInput!) {
              createFactSheet(input: $input) {
                factSheet {
                  id
                  name
                  createdAt
                }
              }
            }
            """;

    /** Mutation that applies patches to a fact sheet. */
    private static final String MUTATION_UPDATE_FS = """
            mutation AddRelationship($id: ID!, $patches: [Patch]!) {
              updateFactSheet(id: $id, patches: $patches) {
                factSheet {
                  id
                }
              }
            }
            """;

    /** REST client of the GraphQL endpoint. */
    @RestClient
    LeanIxGraphQLClient graphQLClient;

    /** Source of the bearer token. */
    @Inject
    LeanIxAuthService authService;

    /** LeanIX connection settings. */
    @Inject
    LeanIxConfig config;

    /** JSON mapper. */
    /** Ids of the subscription roles by name, read from LeanIX on first use. */
    private volatile Map<String, String> roleIds;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Reads all Initiative fact sheets.
     *
     * @return  The Initiatives with id, name, display name and creation time.
     * @throws IllegalStateException If LeanIX reports a GraphQL error.
     */
    public List<FactSheetNode> fetchProjectInitiatives() {
        String bearer = authService.getBearerToken();
        JsonNode response = graphQLClient.execute(bearer, new GraphQLRequest(QUERY_INITIATIVES));

        assertNoErrors(response, "fetchProjectInitiatives");

        List<FactSheetNode> results = new ArrayList<>();
        JsonNode edges = response.path("data").path("allFactSheets").path("edges");
        for (JsonNode edge : edges) {
            JsonNode node = edge.path("node");
            FactSheetNode fs = new FactSheetNode();
            fs.id          = node.path("id").asText(null);
            fs.name        = node.path("name").asText(null);
            fs.displayName = node.path("displayName").asText(null);
            fs.createdAt   = node.path("createdAt").asText(null);
            fs.itDirector     = blankToNull(node.path("itDirector").asText(null));
            fs.itManager      = blankToNull(node.path("itManager").asText(null));
            fs.itManagerEmail = blankToNull(node.path("itManagerEmail").asText(null));
            results.add(fs);
        }
        LOG.debugf("Fetched %d project initiatives from LeanIX", results.size());
        return results;
    }

    /**
     * Reads the ids of the Initiatives that already have an ArchitectureReview in LeanIX.
     * <p>
     * Reads every ArchitectureReview of the workspace, page by page, and collects the Initiatives they are linked to.
     *
     * @return  The Initiative fact sheet ids.
     * @throws IllegalStateException If LeanIX reports a GraphQL error.
     */
    public Set<String> fetchInitiativeIdsWithReview() {
        Set<String> ids = new HashSet<>();
        String after = null;
        boolean more = true;
        while (more) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("after", after);
            JsonNode response = graphQLClient.execute(authService.getBearerToken(),
                    new GraphQLRequest(QUERY_REVIEW_LINKS, variables));
            assertNoErrors(response, "fetchInitiativeIdsWithReview");

            JsonNode result = response.path("data").path("allFactSheets");
            for (JsonNode edge : result.path("edges")) {
                for (JsonNode link : edge.path("node").path(RELATION_TO_INITIATIVE).path("edges")) {
                    String id = link.path("node").path("factSheet").path("id").asText(null);
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
            more = result.path("pageInfo").path("hasNextPage").asBoolean(false);
            after = result.path("pageInfo").path("endCursor").asText(null);
        }
        LOG.debugf("%d Initiatives already have an ArchitectureReview in LeanIX", ids.size());
        return ids;
    }

    /**
     * Creates an ArchitectureReview fact sheet.
     *
     * @param name  The name of the new fact sheet.
     * @return       The id of the new fact sheet.
     * @throws IllegalStateException If LeanIX reports a GraphQL error.
     */
    public String createArchitectureReview(String name) {
        String bearer = authService.getBearerToken();

        Map<String, Object> variables = Map.of(
                "input", Map.of("name", name, "type", "ArchitectureReview"));

        JsonNode response = graphQLClient.execute(bearer,
                new GraphQLRequest(MUTATION_CREATE_ARB, variables));

        assertNoErrors(response, "createArchitectureReview[" + name + "]");

        String id = response.path("data")
                            .path("createFactSheet")
                            .path("factSheet")
                            .path("id")
                            .asText(null);
        LOG.infof("Created ArchitectureReview '%s' with id=%s", name, id);
        return id;
    }

    /**
     * Links an ArchitectureReview fact sheet to an Initiative.
     * <p>
     * Uses the relArchitectureReviewToInitiative relationship.
     *
     * @param arbFsId         The id of the ArchitectureReview fact sheet.
     * @param initiativeFsId  The id of the Initiative fact sheet.
     * @throws IllegalStateException If LeanIX reports a GraphQL error.
     */
    public void linkToInitiative(String arbFsId, String initiativeFsId) {
        String bearer = authService.getBearerToken();

        // Patch path suffix must be unique per-call; use a timestamp-based key
        String patchKey = "new_" + System.currentTimeMillis();
        String patchPath = "/" + RELATION_TO_INITIATIVE + "/" + patchKey;
        String patchValue = "{\"factSheetId\":\"" + initiativeFsId + "\"}";

        Map<String, Object> patch = Map.of(
                "op",    "add",
                "path",  patchPath,
                "value", patchValue);

        Map<String, Object> variables = Map.of(
                "id",      arbFsId,
                "patches", List.of(patch));

        JsonNode response = graphQLClient.execute(bearer,
                new GraphQLRequest(MUTATION_UPDATE_FS, variables));

        assertNoErrors(response, "linkToInitiative[" + arbFsId + " -> " + initiativeFsId + "]");
        LOG.infof("Linked ARB %s to Initiative %s", arbFsId, initiativeFsId);
    }

    /**
     * Sets the ARBPhase field and adds the corresponding tag in a single updateFactSheet call.
     *
     * @param arbFsId  The id of the ArchitectureReview fact sheet.
     * @param phase    The value of the ARBPhase field, for example "Open".
     * @param tagId    The id of the tag to add, taken from the configuration.
     * @throws IllegalStateException If LeanIX reports a GraphQL error.
     */
    public void setPhaseAndTag(String arbFsId, String phase, String tagId) {
        String bearer = authService.getBearerToken();

        List<Map<String, Object>> patches = new ArrayList<>();

        patches.add(Map.of(
                "op",    "replace",
                "path",  "/ARBPhase",
                "value", phase));

        patches.add(Map.of(
                "op",    "add",
                "path",  "/tags",
                "value", "[{\"tagId\":\"" + tagId + "\"}]"));

        Map<String, Object> variables = Map.of(
                "id",      arbFsId,
                "patches", patches);

        JsonNode response = graphQLClient.execute(bearer,
                new GraphQLRequest(MUTATION_UPDATE_FS, variables));

        assertNoErrors(response, "setPhaseAndTag[" + arbFsId + " -> " + phase + "]");
        LOG.infof("Set ARBPhase=%s and tag=%s on ARB %s", phase, tagId, arbFsId);
    }

    /**
     * Checks a GraphQL response for errors.
     *
     * @param response   The JSON response.
     * @param operation  A description of the call, used in the error message.
     * @throws IllegalStateException If the response contains an error; the message holds the first one.
     */
    /**
     * Subscribes a user to a fact sheet with a role.
     * <p>
     * LeanIX allows only certain roles for each subscription type: the Manager role is ACCOUNTABLE, the Solution Architect role
     * RESPONSIBLE and the Enterprise Architect role OBSERVER. The user is found by email address.
     *
     * @param factSheetId  The id of the fact sheet to subscribe to.
     * @param email        The email address of an existing LeanIX user.
     * @param type         The subscription type: ACCOUNTABLE, RESPONSIBLE or OBSERVER.
     * @param roleName     The name of the subscription role, for example "Manager".
     * @throws IllegalStateException If the role does not exist or LeanIX reports a GraphQL error.
     */
    public void subscribe(String factSheetId, String email, String type, String roleName) {
        Map<String, Object> variables = Map.of(
                "factSheetId", factSheetId,
                "user",        Map.of("email", email),
                "type",        type,
                "roles",       List.of(Map.of("id", roleId(roleName))));

        JsonNode response = graphQLClient.execute(authService.getBearerToken(),
                new GraphQLRequest(MUTATION_CREATE_SUBSCRIPTION, variables));

        assertNoErrors(response, "subscribe[" + factSheetId + " -> " + email + " as " + roleName + "]");
        LOG.infof("Subscribed %s to %s as %s (%s)", email, factSheetId, roleName, type);
    }

    /**
     * Returns the id of a subscription role, reading the roles from LeanIX the first time.
     *
     * @param roleName  The name of the role; case does not matter.
     * @return           The role id.
     * @throws IllegalStateException If the workspace has no role with that name.
     */
    private String roleId(String roleName) {
        Map<String, String> ids = roleIds;
        if (ids == null) {
            JsonNode response = graphQLClient.execute(authService.getBearerToken(), new GraphQLRequest(QUERY_ROLES));
            assertNoErrors(response, "roleId");
            ids = new HashMap<>();
            for (JsonNode edge : response.path("data").path("allSubscriptionRoles").path("edges")) {
                ids.put(edge.path("node").path("name").asText().toLowerCase(), edge.path("node").path("id").asText());
            }
            roleIds = ids;
        }
        String id = ids.get(roleName.toLowerCase());
        if (id == null) {
            throw new IllegalStateException("LeanIX has no subscription role named " + roleName);
        }
        return id;
    }

    /**
     * Turns blank text into null.
     *
     * @param value  The text; may be null.
     * @return        The text, or null if it is null or blank.
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void assertNoErrors(JsonNode response, String operation) {
        JsonNode errors = response.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            String msg = errors.get(0).path("message").asText("unknown GraphQL error");
            throw new IllegalStateException("GraphQL error in " + operation + ": " + msg);
        }
    }
}
