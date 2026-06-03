package org.example.cinemabookingsystem.concurrency.model;

import org.example.cinemabookingsystem.concurrency.model.enums.LockType;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Lock(
        String id,
        LockType type,
        LockedTable table,
        long resourceId,
        List<String> transactionHasLockIds,
        Instant acquiredAt
) {
    public Lock {
        transactionHasLockIds = List.copyOf(transactionHasLockIds);
    }

    public static Lock newHeldBy(LockType type, LockedTable table, long resourceId, String transactionId) {
        return new Lock(
                UUID.randomUUID().toString(),
                type,
                table,
                resourceId,
                List.of(transactionId),
                Instant.now()
        );
    }

    public Lock withAdditionalHolder(String transactionId) {
        if (transactionHasLockIds.contains(transactionId)) {
            return this;
        }
        return new Lock(
                id,
                type,
                table,
                resourceId,
                appendImmutable(transactionHasLockIds, transactionId),
                acquiredAt
        );
    }

    public Lock withoutHolder(String transactionId) {
        if (!transactionHasLockIds.contains(transactionId)) {
            return this;
        }
        return new Lock(
                id,
                type,
                table,
                resourceId,
                transactionHasLockIds.stream().filter(h -> !h.equals(transactionId)).toList(),
                acquiredAt
        );
    }

    public boolean isHeldOnlyBy(String transactionId) {
        return transactionHasLockIds.size() == 1 && transactionHasLockIds.contains(transactionId);
    }

    public boolean isHeldBy(String transactionId) {
        return transactionHasLockIds.contains(transactionId);
    }

    private static List<String> appendImmutable(List<String> existing, String toAdd) {
        return java.util.stream.Stream
                .concat(existing.stream(), java.util.stream.Stream.of(toAdd))
                .toList();
    }
}
