/**
 * Knows the users of the LeanIX workspace.
 * <p>
 * Reads the users through the user management API and keeps them for an hour. Used to find the email address of a manager who is only
 * known by name, and to subscribe only users that already exist, so that no user is created by accident.
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
import com.nscorp.entarch.leanix.arb.client.LeanIxMtmClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The users of the LeanIX workspace, looked up by email address or by name.
 */
@ApplicationScoped
public class LeanIxUserService {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(LeanIxUserService.class);

    /** How long the list of users is kept before it is read again. */
    private static final Duration MAX_AGE = Duration.ofHours(1);

    /** Number of users read per request. */
    private static final int PAGE_SIZE = 100;

    /** REST client of the user management API. */
    @RestClient
    LeanIxMtmClient mtmClient;

    /** Source of the bearer token. */
    @Inject
    LeanIxAuthService authService;

    /** The users as read last, or null before the first read. */
    private volatile Users users;

    /** When the users were read. */
    private volatile Instant loadedAt = Instant.EPOCH;

    /** The users of the workspace. */
    private record Users(Set<String> emails, Map<String, Set<String>> emailsByName, Map<String, String[]> namesByEmail) {
    }

    /**
     * Tells whether a user with an email address exists in the workspace.
     *
     * @param email  The email address; case does not matter.
     * @return        True if there is a user with that address.
     * @throws IllegalStateException If the users cannot be read.
     */
    public boolean exists(String email) {
        return current().emails().contains(email.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Finds the first and last name of a user by email address.
     *
     * @param email  The email address; case does not matter.
     * @return        The first and last name, or null if no user has that address or the user has no name.
     * @throws IllegalStateException If the users cannot be read.
     */
    public String[] nameOfEmail(String email) {
        return current().namesByEmail().get(email.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Finds the email address of a user by name.
     *
     * @param name  The name as "First Last" or "Last, First"; a middle name or initial is ignored.
     * @return       The email address, or null if no user or more than one user has that first and last name.
     * @throws IllegalStateException If the users cannot be read.
     */
    public String emailOfName(String name) {
        String key = nameKey(name);
        if (key == null) {
            return null;
        }
        Set<String> found = current().emailsByName().get(key);
        return found != null && found.size() == 1 ? found.iterator().next() : null;
    }

    /**
     * Returns the users, reading them again if they are older than an hour.
     *
     * @return  The users.
     * @throws IllegalStateException If the users cannot be read.
     */
    private synchronized Users current() {
        if (users == null || Instant.now().isAfter(loadedAt.plus(MAX_AGE))) {
            try {
                users = load();
                loadedAt = Instant.now();
            } catch (Exception e) {
                throw new IllegalStateException("Could not read the LeanIX users: " + e.getMessage(), e);
            }
        }
        return users;
    }

    /**
     * Reads all users of the workspace, page by page.
     *
     * @return  The users.
     * @throws Exception If a page cannot be read.
     */
    private Users load() throws Exception {
        String bearer = authService.getBearerToken();
        String workspaceId = workspaceId(bearer);
        Set<String> emails = new HashSet<>();
        Map<String, Set<String>> byName = new HashMap<>();
        Map<String, String[]> names = new HashMap<>();
        int page = 1;
        int seen = 0;
        int total;
        do {
            JsonNode answer = mtmClient.users(bearer, workspaceId, PAGE_SIZE, page++);
            total = answer.path("total").asInt();
            JsonNode data = answer.path("data");
            if (data.isEmpty()) {
                break;
            }
            for (JsonNode user : data) {
                seen++;
                String email = user.path("email").asText("").trim().toLowerCase(Locale.ROOT);
                if (email.isEmpty()) {
                    continue;
                }
                emails.add(email);
                String firstName = user.path("firstName").asText("").trim();
                String lastName = user.path("lastName").asText("").trim();
                if (!firstName.isEmpty() && !lastName.isEmpty()) {
                    names.put(email, new String[] {firstName, lastName});
                }
                String key = nameKey(user.path("firstName").asText("") + " " + user.path("lastName").asText(""));
                if (key != null) {
                    byName.computeIfAbsent(key, k -> new HashSet<>()).add(email);
                }
            }
        } while (seen < total);
        LOG.infof("Read %d LeanIX users", emails.size());
        return new Users(emails, byName, names);
    }

    /**
     * Reads the workspace id from a bearer token.
     *
     * @param bearer  The complete Authorization header value ("Bearer " + access token).
     * @return         The workspace id the token belongs to.
     * @throws Exception If the token has no workspace id.
     */
    private static String workspaceId(String bearer) throws Exception {
        String payload = bearer.substring("Bearer ".length()).split("\\.")[1];
        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(payload));
        String id = claims.path("principal").path("permission").path("workspaceId").asText(null);
        if (id == null) {
            throw new IllegalStateException("The LeanIX token has no workspace id");
        }
        return id;
    }

    /**
     * Turns a name into a key for matching: the first word of the first name and the last name, letters only, lower case.
     *
     * @param name  The name as "First Last" or "Last, First".
     * @return       The key, or null if the name has no first and last name.
     */
    static String nameKey(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String first;
        String last;
        if (name.contains(",")) {
            String[] parts = name.split(",", 2);
            last = parts[0];
            first = parts[1];
        } else {
            String[] words = name.trim().split("\\s+");
            if (words.length < 2) {
                return null;
            }
            first = words[0];
            last = words[words.length - 1];
        }
        first = first.trim().split("\\s+")[0];
        String key = letters(first) + "|" + letters(last);
        return key.startsWith("|") || key.endsWith("|") ? null : key;
    }

    /**
     * Keeps only the letters of a text, in lower case.
     *
     * @param text  The text.
     * @return       The letters a-z of the text in lower case.
     */
    private static String letters(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }
}
