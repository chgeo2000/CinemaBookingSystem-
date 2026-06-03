package org.example.cinemabookingsystem.concurrency.repository;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.model.Transaction;
import org.example.cinemabookingsystem.concurrency.model.enums.TransactionStatus;
import org.example.cinemabookingsystem.concurrency.model.rowmappers.TransactionRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {

    private static final String INSERT_SQL = """
            INSERT INTO tx_transaction (id, status, started_at, ended_at)
            VALUES (:id, :status, :startedAt, :endedAt)
            """;

    private static final String UPDATE_STATUS_SQL = """
            UPDATE tx_transaction
               SET status = :status,
                   ended_at = :endedAt
             WHERE id = :id
            """;

    private static final String SELECT_BY_ID_SQL = """
            SELECT id, status, started_at, ended_at
              FROM tx_transaction
             WHERE id = :id
            """;

    private final JdbcClient jdbcClient;

    public void save(Transaction transaction) {
        jdbcClient.sql(INSERT_SQL)
                .param("id", transaction.id())
                .param("status", transaction.status().name())
                .param("startedAt", Timestamp.from(transaction.startedAt()))
                .param("endedAt", transaction.endedAt() == null ? null : Timestamp.from(transaction.endedAt()))
                .update();
    }

    public void updateStatus(String transactionId, TransactionStatus status, Instant endedAt) {
        jdbcClient.sql(UPDATE_STATUS_SQL)
                .param("id", transactionId)
                .param("status", status.name())
                .param("endedAt", endedAt == null ? null : Timestamp.from(endedAt))
                .update();
    }

    public Optional<Transaction> findById(String transactionId) {
        return jdbcClient.sql(SELECT_BY_ID_SQL)
                .param("id", transactionId)
                .query(new TransactionRowMapper())
                .optional();
    }
}
