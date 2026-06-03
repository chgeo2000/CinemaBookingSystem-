package org.example.cinemabookingsystem.concurrency.engine;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.api.TransactionContext;
import org.example.cinemabookingsystem.concurrency.model.enums.LockType;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

/**
 * Single-use context bound to one transaction running on one thread. Combines lock acquisition
 * (delegated to {@link LockManager}) with logged SQL execution (delegated to
 * {@link OperationExecutor}).
 */
@RequiredArgsConstructor
final class TransactionContextImpl implements TransactionContext {

    private final String transactionId;
    private final LockManager lockManager;
    private final OperationExecutor operationExecutor;

    private int nextSequenceNumber = 1;

    @Override
    public String transactionId() {
        return transactionId;
    }

    @Override
    public void lockShared(LockedTable table, long resourceId) {
        lockManager.acquire(transactionId, table, resourceId, LockType.SHARED);
    }

    @Override
    public void lockExclusive(LockedTable table, long resourceId) {
        lockManager.acquire(transactionId, table, resourceId, LockType.EXCLUSIVE);
    }

    @Override
    public long insert(LockedTable table, String sql, Object... params) {
        return operationExecutor.insert(transactionId, nextSeq(), table, sql, params);
    }

    @Override
    public int update(LockedTable table,
                      long resourceId,
                      String sql,
                      Object[] params,
                      String compensationSql,
                      Object[] compensationParams) {
        return operationExecutor.update(transactionId, nextSeq(), table, resourceId,
                sql, params, compensationSql, compensationParams);
    }

    @Override
    public int delete(LockedTable table,
                      long resourceId,
                      String sql,
                      Object[] params,
                      String compensationSql,
                      Object[] compensationParams) {
        return operationExecutor.delete(transactionId, nextSeq(), table, resourceId,
                sql, params, compensationSql, compensationParams);
    }

    @Override
    public <T> Optional<T> queryOptional(LockedTable table,
                                         long resourceId,
                                         String sql,
                                         RowMapper<T> rowMapper,
                                         Object... params) {
        return operationExecutor.queryOptional(transactionId, nextSeq(), table, resourceId,
                sql, rowMapper, params);
    }

    @Override
    public <T> List<T> queryList(LockedTable table,
                                 long resourceId,
                                 String sql,
                                 RowMapper<T> rowMapper,
                                 Object... params) {
        return operationExecutor.queryList(transactionId, nextSeq(), table, resourceId,
                sql, rowMapper, params);
    }

    private int nextSeq() {
        return nextSequenceNumber++;
    }
}
