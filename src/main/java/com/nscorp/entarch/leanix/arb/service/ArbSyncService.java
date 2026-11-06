/**
 * ArbSyncService.java
 *
 * Creates an ArchitectureReview fact sheet in LeanIX for every new Initiative.
 * <p>
 * Reads the Initiatives from LeanIX, skips those created before the last run or that already have a review, and for each
 * remaining one creates the review, links it to the Initiative, sets its phase and tag, and records it in the database.
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
package com.nscorp.entarch.leanix.arb.service;

import com.nscorp.entarch.leanix.arb.config.LeanIxConfig;
import com.nscorp.entarch.leanix.arb.entity.ArchitectureReview;
import com.nscorp.entarch.leanix.arb.entity.SyncState;
import com.nscorp.entarch.leanix.arb.model.FactSheetNode;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Performs the ARB sync: one ArchitectureReview per new Initiative. */
@ApplicationScoped
public class ArbSyncService {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(ArbSyncService.class);

    /** Access to the LeanIX GraphQL API. */
    /** Value given to the ARBPhase field and name of the tag of a new review. */
    private static final String PHASE_OPEN = "Open";

    @Inject
    LeanIxGraphQLService graphQLService;

    /** LeanIX connection settings. */
    @Inject
    ReviewTeamService reviewTeamService;

    @Inject
    LeanIxConfig config;

    /**
     * Runs the sync; called by the scheduler.
     * <p>
     * Fetches the Initiatives from LeanIX and, for each new one that has no ArchitectureReview in LeanIX yet, creates an ArchitectureReview fact sheet and persists a record.
     * There is no transaction around the whole run, because it makes thousands of REST calls; each database write uses its own
     * short transaction. A failure on one Initiative is logged and the run continues with the next.
     */
    public void sync() {
        LocalDateTime lastRun = QuarkusTransaction.requiringNew().call(() -> {
            SyncState state = SyncState.getSingleton();
            return state.lastRunTs;
        });
        LOG.infof("Starting ARB sync — last run was %s", lastRun);

        List<FactSheetNode> initiatives;
        try {
            initiatives = graphQLService.fetchProjectInitiatives();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch initiatives from LeanIX; sync aborted");
            return;
        }

        fillInitiativeNames(initiatives);
        reviewTeamService.fillNames(initiatives);
        completeReviews(initiatives);

        Set<String> reviewed;
        try {
            reviewed = graphQLService.fetchInitiativeIdsWithReview();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to read the existing ArchitectureReviews from LeanIX; sync aborted");
            return;
        }

        int created = 0;
        int skipped = 0;

        for (FactSheetNode initiative : initiatives) {
            if (!isAfter(initiative.createdAt, lastRun)) {
                skipped++;
                continue;
            }

            if (reviewed.contains(initiative.id)) {
                LOG.debugf("Initiative %s already has an ArchitectureReview in LeanIX — skipping", initiative.id);
                skipped++;
                continue;
            }

            boolean exists = QuarkusTransaction.requiringNew().call(() ->
                    ArchitectureReview.existsByInitiativeId(initiative.id));
            if (exists) {
                LOG.debugf("Initiative %s already has an ARB record — skipping", initiative.id);
                skipped++;
                continue;
            }

            try {
                processInitiative(initiative);
                created++;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to process initiative %s (%s); continuing",
                        initiative.id, initiative.name);
            }
        }

        QuarkusTransaction.requiringNew().run(() -> {
            SyncState state = SyncState.getSingleton();
            state.lastRunTs = LocalDateTime.now();
        });
        LOG.infof("ARB sync complete — created=%d, skipped=%d", created, skipped);
    }

    /**
     * Reads the Initiatives from LeanIX and fills in the Initiative name of the reviews that do not have one yet.
     * <p>
     * Called at startup, so the ARB page shows names for reviews recorded before the name was stored. A failure is logged and
     * ignored; the names are also filled in at the start of every run.
     */
    public void fillInitiativeNames() {
        try {
            List<FactSheetNode> initiatives = graphQLService.fetchProjectInitiatives();
            fillInitiativeNames(initiatives);
            reviewTeamService.fillNames(initiatives);
            completeReviews(initiatives);
        } catch (Exception e) {
            LOG.warnf("Could not fill in the Initiative names: %s", e.getMessage());
        }
    }

    /**
     * Fills in the Initiative name of the reviews that do not have one yet.
     *
     * @param initiatives  The Initiatives read from LeanIX.
     */
    void fillInitiativeNames(List<FactSheetNode> initiatives) {
        Map<String, String> nameById = new HashMap<>();
        initiatives.forEach(i -> nameById.put(i.id, i.name));
        int filled = QuarkusTransaction.requiringNew().call(() -> {
            int count = 0;
            for (ArchitectureReview review : ArchitectureReview.<ArchitectureReview>list("initiativeName is null")) {
                String name = nameById.get(review.initiativeId);
                if (name != null) {
                    review.initiativeName = name.length() > 255 ? name.substring(0, 255) : name;
                    count++;
                }
            }
            return count;
        });
        if (filled > 0) {
            LOG.infof("Filled in the Initiative name of %d reviews", filled);
        }
    }

    /**
     * Creates the review for one Initiative.
     * <p>
     * Creates the fact sheet, links it to the Initiative, saves the database record and then sets the phase and tag. The record is
     * saved in its own transaction before the tagging, so it is kept even if tagging fails.
     *
     * @param initiative  The Initiative to create a review for.
     * @throws IllegalStateException If LeanIX reports an error for one of the calls.
     */
    private void processInitiative(FactSheetNode initiative) {
        // Truncate to column length; VARCHAR(36) may be shorter than the name
        String arbName = truncate(initiative.name, 36);

        // 1. Create ArchitectureReview fact sheet in LeanIX
        String arbFsId = graphQLService.createArchitectureReview(arbName);

        // 2. Link the new ARB to the source Initiative
        graphQLService.linkToInitiative(arbFsId, initiative.id);

        // 3. Persist a local audit record (own transaction so it commits even if tagging fails)
        ArchitectureReview record = QuarkusTransaction.requiringNew().call(() -> {
            ArchitectureReview r = new ArchitectureReview();
            r.arbName      = arbName;
            r.initiativeId = initiative.id;
            r.initiativeName = initiative.name;
            r.arbPhase     = PHASE_OPEN;
            r.arbTag       = PHASE_OPEN;
            r.itManager      = initiative.itManager;
            r.itManagerEmail = initiative.itManagerEmail;
            r.arbFsId      = arbFsId;
            r.persist();
            return r;
        });

        // 4. Set initial ARBPhase and apply corresponding tag
        graphQLService.setPhaseAndTag(arbFsId, PHASE_OPEN, config.tagArbPhaseOpen());

        // 5. Subscribe the manager and the review team; a failure is recorded and does not undo the review
        store(record.arbId, reviewTeamService.assign(arbFsId, initiative.itManager, initiative.itManagerEmail));

        LOG.infof("Processed initiative '%s' -> ARB fact sheet %s (DB id=%s)",
                initiative.name, arbFsId, record.arbId);
    }

    /**
     * Tells whether a LeanIX timestamp is later than a threshold.
     *
     * @param isoTimestamp  The creation time as an ISO 8601 timestamp; may be null.
     * @param threshold     The moment to compare with, in local time.
     * @return               True if the timestamp is later; false if it is missing; true if it cannot be parsed, to be safe.
     */
    /**
     * Records the people subscribed to a review.
     *
     * @param arbId   The primary key of the review record.
     * @param result  What was assigned.
     */
    private void store(UUID arbId, ReviewTeamService.Result result) {
        QuarkusTransaction.requiringNew().run(() -> {
            ArchitectureReview review = ArchitectureReview.findById(arbId);
            review.itManagerEmail = result.managerEmail();
            review.enterpriseArchitect = result.enterpriseArchitect();
            review.solutionArchitect = result.solutionArchitect();
            review.managerStatus = result.managerStatus();
            review.teamStatus = result.teamStatus();
        });
    }

    /**
     * Subscribes the manager and review team to the reviews that were created before this was done.
     * <p>
     * Handles the reviews that have a manager status but no team status, using the Initiatives just read from LeanIX. Each review is
     * done once; its team status is set whatever the outcome.
     *
     * @param initiatives  The Initiatives read from LeanIX.
     */
    void completeReviews(List<FactSheetNode> initiatives) {
        List<ArchitectureReview> open = QuarkusTransaction.requiringNew().call(() ->
                ArchitectureReview.<ArchitectureReview>list("teamStatus is null and managerStatus is not null"));
        if (open.isEmpty()) {
            return;
        }
        Map<String, FactSheetNode> byId = new HashMap<>();
        initiatives.forEach(i -> byId.put(i.id, i));
        int done = 0;
        for (ArchitectureReview review : open) {
            FactSheetNode initiative = byId.get(review.initiativeId);
            if (initiative == null || review.arbFsId == null) {
                continue;
            }
            try {
                store(review.arbId, reviewTeamService.assign(review.arbFsId, initiative.itManager, initiative.itManagerEmail));
                done++;
            } catch (Exception e) {
                LOG.errorf(e, "Could not complete the review %s; continuing", review.arbFsId);
            }
        }
        LOG.infof("Subscribed the manager and review team on %d of %d earlier reviews", done, open.size());
    }

    private boolean isAfter(String isoTimestamp, LocalDateTime threshold) {
        if (isoTimestamp == null || isoTimestamp.isBlank()) {
            return false;
        }
        try {
            LocalDateTime createdAt = OffsetDateTime.parse(isoTimestamp)
                    .toLocalDateTime();
            return createdAt.isAfter(threshold);
        } catch (DateTimeParseException e) {
            LOG.warnf("Cannot parse createdAt '%s'; including record to be safe", isoTimestamp);
            return true;
        }
    }

    /**
     * Cuts a value to a maximum length.
     *
     * @param value   The text; may be null.
     * @param maxLen  The maximum number of characters.
     * @return         The text, shortened if needed, or an empty string if it is null.
     */
    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
