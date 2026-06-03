package org.example.cinemabookingsystem.concurrency.repository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.model.Operation;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.example.cinemabookingsystem.concurrency.model.enums.OperationType;
import org.example.cinemabookingsystem.concurrency.model.rowmappers.OperationRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.example.cinemabookingsystem.utils.JsonParser.toJsonString;

@Repository
@RequiredArgsConstructor
public class OperationRepository {

    private static final String INSERT_SQL = """
            INSERT INTO tx_operation
                (transaction_id, sequence_number, operation_type, table_name,
                 resource_id, compensation_sql, compensation_params_json, executed_at)
            VALUES
                (:transactionId, :sequenceNumber, :operationType, :tableName,
                 :resourceId, :compensationSql, CAST(:compensationParamsJson AS jsonb), :executedAt)
            """;

    private static final String SELECT_FOR_ROLLBACK_SQL = """
            SELECT id, transaction_id, sequence_number, operation_type, table_name,
                   resource_id, compensation_sql, compensation_params_json, executed_at
              FROM tx_operation
             WHERE transaction_id = :transactionId
             ORDER BY sequence_number DESC
            """;

    private final JdbcClient jdbcClient;

    public void append(String transactionId,
                       int sequenceNumber,
                       OperationType type,
                       LockedTable table,
                       Long resourceId,
                       String compensationSql,
                       JsonNode compensationParams) {
        jdbcClient.sql(INSERT_SQL)
                .param("transactionId", transactionId)
                .param("sequenceNumber", sequenceNumber)
                .param("operationType", type.name())
                .param("tableName", table.name())
                .param("resourceId", resourceId)
                .param("compensationSql", compensationSql)
                .param("compensationParamsJson", toJsonString(compensationParams))
                .param("executedAt", Timestamp.from(Instant.now()))
                .update();
    }

    public List<Operation> findForRollback(String transactionId) {
        return jdbcClient.sql(SELECT_FOR_ROLLBACK_SQL)
                .param("transactionId", transactionId)
                .query(new OperationRowMapper())
                .list();
    }
}
