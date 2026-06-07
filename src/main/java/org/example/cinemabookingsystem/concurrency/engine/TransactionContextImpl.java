package org.example.cinemabookingsystem.concurrency.engine;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.api.Compensation;
import org.example.cinemabookingsystem.concurrency.api.TransactionContext;
import org.example.cinemabookingsystem.concurrency.model.enums.LockType;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;

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
                      Compensation compensation,
                      Object... params) {
        return operationExecutor.update(transactionId, nextSeq(), table, resourceId,
                sql, compensation, params);
    }

    @Override
    public int delete(LockedTable table,
                      long resourceId,
                      String sql,
                      Compensation compensation,
                      Object... params) {
        return operationExecutor.delete(transactionId, nextSeq(), table, resourceId,
                sql, compensation, params);
    }

    @Override
    public boolean exists(LockedTable table, long resourceId, String sql, Object... params) {
        return operationExecutor.exists(transactionId, nextSeq(), table, resourceId, sql, params);
    }

    private int nextSeq() {
        return nextSequenceNumber++;
    }
}
