package org.example.cinemabookingsystem.concurrency.engine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.example.cinemabookingsystem.concurrency.model.enums.OperationType;
import org.example.cinemabookingsystem.concurrency.repository.OperationRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                      Object[] params,
                      String compensationSql,
                      Object[] compensationParams) {
        int rows = jdbcClient.sql(sql).params(params).update();
        operationRepository.append(transactionId, sequenceNumber, OperationType.UPDATE, table,
                resourceId, compensationSql, toJsonArray(compensationParams));
        return rows;
    }

    public int delete(String transactionId,
                      int sequenceNumber,
                      LockedTable table,
                      Long resourceId,
                      String sql,
                      Object[] params,
                      String compensationSql,
                      Object[] compensationParams) {
        int rows = jdbcClient.sql(sql).params(params).update();
        operationRepository.append(transactionId, sequenceNumber, OperationType.DELETE, table,
                resourceId, compensationSql, toJsonArray(compensationParams));
        return rows;
    }

    public <T> Optional<T> queryOptional(String transactionId,
                                         int sequenceNumber,
                                         LockedTable table,
                                         Long resourceId,
                                         String sql,
                                         RowMapper<T> rowMapper,
                                         Object[] params) {
        Optional<T> result = jdbcClient.sql(sql).params(params).query(rowMapper).optional();
        logSelect(transactionId, sequenceNumber, table, resourceId);
        return result;
    }

    public <T> List<T> queryList(String transactionId,
                                 int sequenceNumber,
                                 LockedTable table,
                                 Long resourceId,
                                 String sql,
                                 RowMapper<T> rowMapper,
                                 Object[] params) {
        List<T> result = jdbcClient.sql(sql).params(params).query(rowMapper).list();
        logSelect(transactionId, sequenceNumber, table, resourceId);
        return result;
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
