package org.example.cinemabookingsystem.concurrency.model;

import org.example.cinemabookingsystem.concurrency.model.enums.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

public record Transaction(
        String id,
        Instant startedAt,
        Instant endedAt,
        TransactionStatus status
) {
    public static Transaction newActive() {
        return new Transaction(UUID.randomUUID().toString(), Instant.now(), null, TransactionStatus.ACTIVE);
    }

    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(id, startedAt, newStatus == TransactionStatus.ACTIVE ? null : Instant.now(), newStatus);
    }
}
