package org.example.cinemabookingsystem.concurrency.repository;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.model.enums.LockType;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists lock acquisition/release as a durable audit trail.
 *
 * <p>One row per (transaction, resource) — SHARED locks held by multiple transactions
 * produce one row per holder. Authoritative concurrency decisions are made in-memory by
 * {@code LockManager}; this repository exists for durability and post-mortem inspection.
 */
@Repository
@RequiredArgsConstructor
public class LockRepository {

    private static final String INSERT_SQL = """
            INSERT INTO tx_lock (id, transaction_id, table_name, resource_id, lock_type, acquired_at)
            VALUES (:id, :transactionId, :tableName, :resourceId, :lockType, :acquiredAt)
            """;

    private static final String DELETE_BY_TRANSACTION_SQL = """
            DELETE FROM tx_lock
             WHERE transaction_id = :transactionId
            """;

    private final JdbcClient jdbcClient;

    public void recordAcquired(String transactionId, LockedTable table, long resourceId, LockType type) {
        jdbcClient.sql(INSERT_SQL)
                .param("id", UUID.randomUUID().toString())
                .param("transactionId", transactionId)
                .param("tableName", table.name())
                .param("resourceId", resourceId)
                .param("lockType", type.name())
                .param("acquiredAt", Timestamp.from(Instant.now()))
                .update();
    }

    public int deleteAllForTransaction(String transactionId) {
        return jdbcClient.sql(DELETE_BY_TRANSACTION_SQL)
                .param("transactionId", transactionId)
                .update();
    }
}
