package org.example.cinemabookingsystem.concurrency.engine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.api.Compensation;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.example.cinemabookingsystem.concurrency.model.enums.OperationType;
import org.example.cinemabookingsystem.concurrency.repository.OperationRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.example.cinemabookingsystem.utils.JsonParser.toJsonArray;

/**
 * Executes SQL on behalf of an active transaction and appends an entry to the operation log
 * so the {@link RollbackManager} can compensate on abort.
 *
 * <p>This component does not acquire locks — callers (the {@code TransactionContext}
 * implementation) must lock the relevant resource before invoking any write method, per
 * Strict 2PL.
 */
@Component
@RequiredArgsConstructor
public class OperationExecutor {

    private final JdbcClient jdbcClient;
    private final OperationRepository operationRepository;

    public long insert(String transactionId,
                       int sequenceNumber,
                       LockedTable table,
                       String sql,
                       Object[] params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(sql)
                .params(params)
                .update(keyHolder);

        long generatedId = extractGeneratedId(keyHolder);
        String compensationSql = "DELETE FROM " + table.sqlTableName() + " WHERE id = ?";
        JsonNode compensationParams = toJsonArray(new Object[]{generatedId});
        operationRepository.append(transactionId, sequenceNumber, OperationType.INSERT, table,
                generatedId, compensationSql, compensationParams);

        return generatedId;
    }

    public int update(String transactionId,
                      int sequenceNumber,
                      LockedTable table,
                      Long resourceId,
                      String sql,
                      Compensation compensation,
                      Object[] params) {
        int rows = jdbcClient.sql(sql).params(params).update();
        operationRepository.append(transactionId, sequenceNumber, OperationType.UPDATE, table,
                resourceId, compensation.sql(), toJsonArray(compensation.params().toArray()));
        return rows;
    }

    public int delete(String transactionId,
                      int sequenceNumber,
                      LockedTable table,
                      Long resourceId,
                      String sql,
                      Compensation compensation,
                      Object[] params) {
        int rows = jdbcClient.sql(sql).params(params).update();
        operationRepository.append(transactionId, sequenceNumber, OperationType.DELETE, table,
                resourceId, compensation.sql(), toJsonArray(compensation.params().toArray()));
        return rows;
    }

    public boolean exists(String transactionId,
                          int sequenceNumber,
                          LockedTable table,
                          Long resourceId,
                          String sql,
                          Object[] params) {
        boolean present = Boolean.TRUE.equals(
                jdbcClient.sql(sql).params(params).query(Boolean.class).single());
        logSelect(transactionId, sequenceNumber, table, resourceId);
        return present;
    }

    private void logSelect(String transactionId, int sequenceNumber, LockedTable table, Long resourceId) {
        operationRepository.append(transactionId, sequenceNumber, OperationType.SELECT, table,
                resourceId, null, toJsonArray(new Object[0]));
    }

    private long extractGeneratedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("INSERT did not return any generated keys");
        }
        Object idValue = keys.get("id");
        if (idValue == null && keys.size() == 1) {
            idValue = keys.values().iterator().next();
        }
        if (idValue instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Could not extract numeric id from generated keys: " + keys);
    }
}
