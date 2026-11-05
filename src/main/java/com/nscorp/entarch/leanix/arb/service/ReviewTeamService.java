/**
 * Subscribes the manager and the review team to a new ArchitectureReview.
 * <p>
 * The manager is the one named on the Initiative (the itManagerEmail attribute, or the itManager name looked up among the LeanIX users).
 * The Enterprise Architect and Solution Architect come from the review_team table, the row of the manager.
 * Only users that exist in LeanIX are subscribed, so no user is created by accident.
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

import com.nscorp.entarch.leanix.arb.entity.ReviewTeam;
import com.nscorp.entarch.leanix.arb.model.FactSheetNode;
import com.nscorp.entarch.leanix.arb.client.ConfigServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Assigns the Manager, Enterprise Architect and Solution Architect subscriptions of a review.
 */
@ApplicationScoped
public class ReviewTeamService {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(ReviewTeamService.class);

    /** The address LeanIX shows for a user that was deleted. */
    private static final String DELETED_USER = "deleted@leanix.net";

    /** Longest status text the database column holds. */
    private static final int MAX_STATUS = 255;

    /** Access to the LeanIX GraphQL API. */
    @Inject
    LeanIxGraphQLService graphQLService;

    /** The users of the LeanIX workspace. */
    @Inject
    LeanIxUserService userService;

    /** The review teams are configuration; they are read and updated through the configuration service. */
    @Inject
    @RestClient
    ConfigServiceClient config;

    /**
     * What was assigned to a review.
     *
     * @param managerEmail         The email address of the manager, or null if it could not be found.
     * @param enterpriseArchitect  The Enterprise Architects of the team, separated by ';', or null.
     * @param solutionArchitect    The Solution Architects of the team, separated by ';', or null.
     * @param managerStatus        The outcome for the manager: Assigned, No manager, Manager not found in LeanIX, or Failed.
     * @param teamStatus           The outcome for the Enterprise and Solution Architects.
     */
    public record Result(String managerEmail, String enterpriseArchitect, String solutionArchitect, String managerStatus,
                         String teamStatus) {
    }

    /**
     * Subscribes the manager, the Enterprise Architects and the Solution Architects to a review.
     * <p>
     * A failure on one subscription is recorded in the result and does not stop the others.
     *
     * @param reviewFsId       The id of the ArchitectureReview fact sheet.
     * @param itManager        The itManager attribute of the Initiative, or null.
     * @param itManagerEmail   The itManagerEmail attribute of the Initiative, or null.
     * @return                  What was found and subscribed.
     */
    public Result assign(String reviewFsId, String itManager, String itManagerEmail) {
        String managerEmail = itManagerEmail;
        if (managerEmail == null && itManager != null) {
            try {
                managerEmail = userService.emailOfName(itManager);
            } catch (Exception e) {
                return new Result(null, null, null, status("Failed: " + e.getMessage()), "No manager");
            }
            if (managerEmail == null) {
                managerEmail = emailFromTeams(itManager);
            }
            if (managerEmail == null) {
                return new Result(null, null, null, status("Manager not found in LeanIX: " + itManager), "No manager");
            }
        }
        if (managerEmail == null) {
            return new Result(null, null, null, "No manager", "No manager");
        }

        String managerStatus = subscribeAll(reviewFsId, List.of(managerEmail), "ACCOUNTABLE", "Manager");

        String lookup = managerEmail;
        ReviewTeam team = findMostCommon(lookup);
        if (team == null) {
            return new Result(managerEmail, null, null, managerStatus, "No team for manager");
        }
        String architects = subscribeAll(reviewFsId, split(team.enterpriseArchitect), "OBSERVER", "Enterprise Architect");
        String solution = subscribeAll(reviewFsId, split(team.solutionArchitect), "RESPONSIBLE", "Solution Architect");
        String teamStatus = architects.equals(solution) ? architects
                : status("Enterprise Architect: " + architects + "; Solution Architect: " + solution);
        return new Result(managerEmail, team.enterpriseArchitect, team.solutionArchitect, managerStatus, teamStatus);
    }

    /**
     * Fills in the first and last name of the Director and the Manager of the teams that do not have them yet.
     * <p>
     * The names come from the itDirector and itManager attributes of the Initiatives. A manager is found by the itManagerEmail of an
     * Initiative, or else by comparing the name with the part of the manager's address before the @; a director is found by that
     * comparison, because an Initiative has no director address. Names that were typed on the configuration page are not changed.
     *
     * @param initiatives  The Initiatives read from LeanIX.
     * @return              The number of rows that were changed.
     */
    public int fillNames(List<FactSheetNode> initiatives) {
        Map<String, String[]> managersByEmail = new HashMap<>();
        Map<String, String[]> managersByKey = new HashMap<>();
        Map<String, String[]> directorsByKey = new HashMap<>();
        for (FactSheetNode initiative : initiatives) {
            String[] manager = splitName(initiative.itManager);
            if (manager != null) {
                if (initiative.itManagerEmail != null) {
                    managersByEmail.putIfAbsent(initiative.itManagerEmail.trim().toLowerCase(Locale.ROOT), manager);
                }
                managersByKey.putIfAbsent(key(manager), manager);
            }
            String[] director = splitName(initiative.itDirector);
            if (director != null) {
                directorsByKey.putIfAbsent(key(director), director);
            }
        }
        int changed;
        try {
            changed = fillTeamNames(managersByEmail, managersByKey, directorsByKey);
        } catch (Exception e) {
            LOG.warnf("Could not fill in the Director and Manager names: %s", e.getMessage());
            return 0;
        }
        if (changed > 0) {
            LOG.infof("Filled in the Director and Manager names of %d teams", changed);
        }
        return changed;
    }

    /**
     * Fills in the missing first and last names of the Director and Manager of each team from the Initiatives, and of the architects
     * from the LeanIX users, and saves each team that changed through the configuration service.
     *
     * @param managersByEmail  Manager names by email address.
     * @param managersByKey    Manager names by the key of their name.
     * @param directorsByKey   Director names by the key of their name.
     * @return                  How many teams changed.
     */
    private int fillTeamNames(Map<String, String[]> managersByEmail, Map<String, String[]> managersByKey,
                              Map<String, String[]> directorsByKey) {
        int count = 0;
        for (ReviewTeam team : config.reviewTeams()) {
            boolean touched = false;
            if (team.managerFirstName == null && team.managerLastName == null && team.manager != null) {
                String[] name = managersByEmail.get(team.manager.toLowerCase(Locale.ROOT));
                if (name == null) {
                    name = managersByKey.get(emailKey(team.manager));
                }
                if (name != null) {
                    team.managerFirstName = name[0];
                    team.managerLastName = name[1];
                    touched = true;
                }
            }
            if (team.directorFirstName == null && team.directorLastName == null && team.director != null) {
                String[] name = directorsByKey.get(emailKey(team.director.split(";")[0]));
                if (name != null) {
                    team.directorFirstName = name[0];
                    team.directorLastName = name[1];
                    touched = true;
                }
            }
            touched |= fillArchitects(team);
            if (touched) {
                config.updateReviewTeam(team.id, team);
                count++;
            }
        }
        return count;
    }

    /**
     * Finds the team of a manager.
     *
     * @param managerEmail  The email address of the manager.
     * @return               The row with the highest review count for that manager (the lowest id on a tie), or null if there is none.
     */
    ReviewTeam findMostCommon(String managerEmail) {
        return config.reviewTeams().stream()
                .filter(team -> team.manager != null && team.manager.equalsIgnoreCase(managerEmail))
                .min(java.util.Comparator.comparing((ReviewTeam team) -> team.reviewCount == null ? 0 : team.reviewCount).reversed()
                        .thenComparing(team -> team.id))
                .orElse(null);
    }

    /**
     * Lists the email addresses of all managers that have a team.
     *
     * @return  The manager addresses.
     */
    List<String> managers() {
        return config.reviewTeams().stream().map(team -> team.manager).filter(java.util.Objects::nonNull).distinct().toList();
    }

    /**
     * Fills in the first and last names of the Enterprise Architects and Solution Architects of a team that do not have them yet.
     * <p>
     * An Initiative does not name the architects, so the names are those of the LeanIX users with the addresses of the team. An
     * address that is not a LeanIX user gets the name read from the part before the @, for example kiran.chahar3@nscorp.com is Kiran
     * Chahar; the page can correct it.
     *
     * @param team  The team to complete.
     * @return       True if a name was filled in.
     */
    private boolean fillArchitects(ReviewTeam team) {
        boolean touched = false;
        if (team.enterpriseArchitect != null && team.enterpriseArchitectFirstName == null && team.enterpriseArchitectLastName == null) {
            String[] names = namesOf(team.enterpriseArchitect);
            team.enterpriseArchitectFirstName = names[0];
            team.enterpriseArchitectLastName = names[1];
            touched = true;
        }
        if (team.solutionArchitect != null && team.solutionArchitectFirstName == null && team.solutionArchitectLastName == null) {
            String[] names = namesOf(team.solutionArchitect);
            team.solutionArchitectFirstName = names[0];
            team.solutionArchitectLastName = names[1];
            touched = true;
        }
        return touched;
    }

    /**
     * Finds the names that go with a list of email addresses.
     *
     * @param emails  The addresses, separated by ';'.
     * @return         Two entries: the first names and the last names, each separated by ';' in the order of the addresses (a person
     *                 whose name cannot be found leaves an empty place).
     */
    private String[] namesOf(String emails) {
        List<String> first = new ArrayList<>();
        List<String> last = new ArrayList<>();
        for (String email : split(emails)) {
            String[] name = null;
            try {
                name = userService.nameOfEmail(email);
            } catch (Exception e) {
                LOG.debugf("Could not look up the user %s: %s", email, e.getMessage());
            }
            if (name == null) {
                name = nameFromEmail(email);
            }
            first.add(name == null ? "" : name[0]);
            last.add(name == null ? "" : name[1]);
        }
        return new String[] {String.join(";", first), String.join(";", last)};
    }

    /**
     * Reads a name from an email address like first.last@host.
     *
     * @param email  The email address.
     * @return        The first and last name with a capital letter, or null if the part before the @ is not first.last.
     */
    static String[] nameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        String[] parts = email.substring(0, email.indexOf('@')).replaceAll("[0-9]", "").split("[._]");
        if (parts.length < 2 || parts[0].isEmpty() || parts[parts.length - 1].isEmpty()) {
            return null;
        }
        return new String[] {capital(parts[0]), capital(parts[parts.length - 1])};
    }

    /**
     * Gives a word a capital first letter.
     *
     * @param word  The word.
     * @return       The word with a capital first letter and the rest in lower case.
     */
    private static String capital(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Splits the name of an Initiative attribute into first and last name.
     *
     * @param name  The name as "Last; First M." (the form of the itDirector and itManager attributes), "Last, First" or "First Last".
     * @return       The first and last name, with middle initials removed, or null if the name has no first and last name.
     */
    static String[] splitName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String last;
        String given;
        int separator = name.indexOf(';') >= 0 ? name.indexOf(';') : name.indexOf(',');
        if (separator >= 0) {
            last = name.substring(0, separator).trim();
            given = name.substring(separator + 1).trim();
        } else {
            String[] words = name.trim().split("\\s+");
            if (words.length < 2) {
                return null;
            }
            last = words[words.length - 1];
            given = String.join(" ", java.util.Arrays.copyOf(words, words.length - 1));
        }
        String first = null;
        for (String word : given.split("\\s+")) {
            String bare = word.replace(".", "");
            if (bare.length() > 1) {
                first = bare;
                break;
            }
        }
        if (first == null) {
            first = given.split("\\s+")[0].replace(".", "");
        }
        return first.isEmpty() || last.isEmpty() ? null : new String[] {first, last};
    }

    /**
     * Makes the key that matches a name with an email address: first and last name, letters only, lower case.
     *
     * @param name  The first and last name.
     * @return       The key.
     */
    private static String key(String[] name) {
        return name[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "") + "|" + name[1].toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    /**
     * Makes the key of an email address like first.last@host or first.m.last2@host.
     *
     * @param email  The email address.
     * @return        The key from the first and last part before the @ (digits ignored), or null if there is one part only.
     */
    private static String emailKey(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        String[] parts = email.substring(0, email.indexOf('@')).replaceAll("[0-9]", "").trim().split("[._]");
        if (parts.length < 2) {
            return null;
        }
        return key(new String[] {parts[0], parts[parts.length - 1]});
    }

    /**
     * Finds the email address of a manager by name among the managers of the review_team table.
     * <p>
     * The name is compared with the part of the address before the @, for example "Kiran Chahar" with kiran.chahar3@nscorp.com.
     * Used when the manager is not a LeanIX user, so the team can still be found.
     *
     * @param name  The name as "First Last" or "Last, First".
     * @return       The email address, or null if no manager or more than one manager has that name.
     */
    private String emailFromTeams(String name) {
        String key = LeanIxUserService.nameKey(name);
        if (key == null) {
            return null;
        }
        List<String> found = new ArrayList<>();
        for (String email : managers()) {
            String local = email.substring(0, email.indexOf('@')).replace('.', ' ').replaceAll("[0-9]", "");
            if (key.equals(LeanIxUserService.nameKey(local))) {
                found.add(email.toLowerCase(Locale.ROOT));
            }
        }
        return found.size() == 1 ? found.get(0) : null;
    }

    /**
     * Subscribes a list of users with one role and describes what happened.
     *
     * @param reviewFsId  The id of the ArchitectureReview fact sheet.
     * @param emails      The email addresses of the users to subscribe.
     * @param type        The subscription type.
     * @param roleName    The name of the subscription role.
     * @return             "Assigned" if every user was subscribed, "None in team" if there was nobody to subscribe, otherwise
     *                     how many were assigned, which users are not in LeanIX and which subscriptions failed.
     */
    private String subscribeAll(String reviewFsId, List<String> emails, String type, String roleName) {
        List<String> notFound = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        int assigned = 0;
        int total = 0;
        for (String email : emails) {
            if (email.equalsIgnoreCase(DELETED_USER)) {
                continue;
            }
            total++;
            try {
                if (!userService.exists(email)) {
                    notFound.add(email);
                    continue;
                }
                graphQLService.subscribe(reviewFsId, email, type, roleName);
                assigned++;
            } catch (Exception e) {
                LOG.warnf("Could not subscribe %s to %s as %s: %s", email, reviewFsId, roleName, e.getMessage());
                failed.add(email + " (" + e.getMessage() + ")");
            }
        }
        if (total == 0) {
            return "None in team";
        }
        if (notFound.isEmpty() && failed.isEmpty()) {
            return "Assigned";
        }
        StringBuilder text = new StringBuilder("Assigned ").append(assigned).append(" of ").append(total);
        if (!notFound.isEmpty()) {
            text.append("; not in LeanIX: ").append(String.join(", ", notFound));
        }
        if (!failed.isEmpty()) {
            text.append("; failed: ").append(String.join(", ", failed));
        }
        return status(text.toString());
    }

    /**
     * Splits a list of email addresses separated by ';'.
     *
     * @param cell  The text; may be null.
     * @return       The addresses, trimmed and without empty entries.
     */
    private static List<String> split(String cell) {
        List<String> emails = new ArrayList<>();
        if (cell != null) {
            for (String part : cell.split(";")) {
                if (!part.isBlank()) {
                    emails.add(part.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return emails;
    }

    /**
     * Cuts a status text to the length of its database column.
     *
     * @param text  The text.
     * @return       The text, shortened if needed.
     */
    private static String status(String text) {
        return text.length() <= MAX_STATUS ? text : text.substring(0, MAX_STATUS);
    }
}
