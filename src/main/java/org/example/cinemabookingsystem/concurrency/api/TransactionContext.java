package org.example.cinemabookingsystem.concurrency.api;

import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;

/**
 * Scoped handle exposed to business code while a transaction is active.
 *
 * <p>Usage pattern follows Strict 2PL: acquire the appropriate lock first, then execute the SQL.
 * The context records every executed write in the operation log so the {@code RollbackManager}
 * can replay compensations in reverse order if the transaction aborts.
 *
 * <p>The context is bound to a single thread (the one running inside {@link TransactionManager#execute}).
 * It must not be passed to other threads.
 *
 * <p><b>SQL parameter convention:</b> the SQL bind parameters are always passed as trailing
 * {@code Object... params} varargs, mirroring Spring's {@code JdbcClient.params(Object...)}.
 * For {@link #update} and {@link #delete} the undo action is bundled into a {@link Compensation}
 * value placed before {@code params}, so the main parameters stay in final (varargs) position.
 */
public interface TransactionContext {

    String transactionId();

    void lockShared(LockedTable table, long resourceId);

    void lockExclusive(LockedTable table, long resourceId);

    /**
     * INSERTs a row and returns the generated identity column. Auto-builds the compensation
     * {@code DELETE FROM <table> WHERE id = ?} using the returned key.
     */
    long insert(LockedTable table, String sql, Object... params);

    /**
     * UPDATEs rows and logs the supplied {@link Compensation}. Caller is responsible
     * for snapshotting old values needed to undo the change.
     */
    int update(LockedTable table,
               long resourceId,
               String sql,
               Compensation compensation,
               Object... params);

    /**
     * DELETEs rows and logs the supplied {@link Compensation} (typically an INSERT
     * with the row's prior values).
     */
    int delete(LockedTable table,
               long resourceId,
               String sql,
               Compensation compensation,
               Object... params);

    /**
     * Runs an existence check under the transaction (logged as a SELECT operation). The supplied
     * {@code sql} must return a single boolean column — e.g.
     * {@code SELECT EXISTS(SELECT 1 FROM <table> WHERE ...)}.
     */
    boolean exists(LockedTable table, long resourceId, String sql, Object... params);
}
