package org.example.cinemabookingsystem.concurrency.model;

import org.example.cinemabookingsystem.concurrency.model.enums.LockType;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable snapshot of a lock held on a single resource.
 *
 * <p>In Strict 2PL a lock is associated with one resource ({@code table + resourceId}) but
 * may be held by multiple transactions simultaneously when the mode is {@link LockType#SHARED}.
 * The {@code transactionsHoldingLockIds} list carries all current holders; for an
 * {@link LockType#EXCLUSIVE} lock it always contains exactly one entry.
 *
 * <p>Every mutating operation ({@link #addHolder}, {@link #removeHolder}) returns
 * a new instance — the record is never modified in place.
 */
public record Lock(
        String id,
        LockType type,
        LockedTable table,
        long resourceId,
        List<String> transactionsHoldingLockIds,
        Instant acquiredAt
) {
    /**
     * Compact constructor that defensively copies the holder list so that the caller's
     * collection cannot mutate the record after construction.
     *
     * <p>{@code List.copyOf} is required here, not optional. Records expose their components
     * directly via accessors, so without the copy a caller passing a mutable {@code ArrayList}
     * could modify the list after construction, silently corrupting the lock's holder set and
     * breaking the {@link org.example.cinemabookingsystem.concurrency.engine.LockManager}'s
     * concurrency invariants. The copy guarantees the record is truly immutable once built.
     */
    public Lock {
        transactionsHoldingLockIds = List.copyOf(transactionsHoldingLockIds);
    }

    /**
     * Creates a new lock granted exclusively to {@code transactionId} on the given resource.
     */
    public static Lock grantedTo(LockType type, LockedTable table, long resourceId, String transactionId) {
        return new Lock(
                UUID.randomUUID().toString(),
                type,
                table,
                resourceId,
                List.of(transactionId),
                Instant.now()
        );
    }

    /**
     * Returns a copy of this lock with {@code transactionId} added to the holder list.
     * Used when a second transaction is granted a compatible {@link LockType#SHARED} lock
     * on the same resource. Returns {@code this} unchanged if the transaction is already a holder.
     */
    public Lock addHolder(String transactionId) {
        if (transactionsHoldingLockIds.contains(transactionId)) {
            return this;
        }
        var mutableTransactionsHoldingLockIds = new ArrayList<>(transactionsHoldingLockIds);
        mutableTransactionsHoldingLockIds.add(transactionId);
        return new Lock(id, type, table, resourceId, mutableTransactionsHoldingLockIds, acquiredAt);
    }

    /**
     * Returns a copy of this lock with {@code transactionId} removed from the holder list.
     * Called on commit or abort when the transaction releases its locks.
     * Returns {@code this} unchanged if the transaction is not currently a holder.
     */
    public Lock removeHolder(String transactionId) {
        if (!transactionsHoldingLockIds.contains(transactionId)) {
            return this;
        }
        return new Lock(
                id,
                type,
                table,
                resourceId,
                transactionsHoldingLockIds.stream().filter(h -> !h.equals(transactionId)).toList(),
                acquiredAt
        );
    }

    /**
     * Returns {@code true} if {@code transactionId} is the sole holder of this lock.
     * Used to determine whether a SHARED lock can be upgraded to EXCLUSIVE without conflict.
     */
    public boolean isHeldOnlyBy(String transactionId) {
        return transactionsHoldingLockIds.size() == 1 && transactionsHoldingLockIds.contains(transactionId);
    }

    /**
     * Returns {@code true} if {@code transactionId} is among the current holders of this lock.
     */
    public boolean isHeldBy(String transactionId) {
        return transactionsHoldingLockIds.contains(transactionId);
    }

}
