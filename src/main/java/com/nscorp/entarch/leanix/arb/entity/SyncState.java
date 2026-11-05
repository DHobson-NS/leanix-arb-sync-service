/**
 * Database record of the state of the ARB sync.
 * <p>
 * Stored in the table sync_state of the arb schema as a single row that holds the time of the
 * last run. Initiatives created before that time are not processed again.
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

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** State of the ARB sync; only one row (id 1) exists. */
@Entity
@Table(name = "sync_state")
public class SyncState extends PanacheEntityBase {

    /** Primary key; always 1. */
    @Id
    @Column(name = "id")
    public Integer id;

    /** When the last run of the sync finished. */
    @Column(name = "last_run_ts", nullable = false)
    public LocalDateTime lastRunTs;

    /**
     * Returns the single state row, creating it if it does not exist.
     *
     * @return  The row with id 1; a new one holds the current time as its last run.
     */
    public static SyncState getSingleton() {
        SyncState state = findById(1);
        if (state == null) {
            state = new SyncState();
            state.id = 1;
            state.lastRunTs = LocalDateTime.now();
            state.persist();
        }
        return state;
    }
}
