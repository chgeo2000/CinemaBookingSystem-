package org.example.cinemabookingsystem.concurrency.api;

import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

/**
 * Scoped handle exposed to business code while a transaction is active.
 *
 * <p>Usage pattern follows Strict 2PL: acquire the appropriate lock first, then execute the SQL.
 * The context records every executed write in the operation log so the {@code RollbackManager}
 * can replay compensations in reverse order if the transaction aborts.
 *
 * <p>The context is bound to a single thread (the one running inside {@link TransactionManager#execute}).
 * It must not be passed to other threads.
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
     * UPDATEs rows and logs a caller-supplied compensation SQL. Caller is responsible
     * for snapshotting old values needed to undo the change.
     */
    int update(LockedTable table,
               long resourceId,
               String sql,
               Object[] params,
               String compensationSql,
               Object[] compensationParams);

    /**
     * DELETEs rows and logs a caller-supplied compensation SQL (typically an INSERT
     * with the row's prior values).
     */
    int delete(LockedTable table,
               long resourceId,
               String sql,
               Object[] params,
               String compensationSql,
               Object[] compensationParams);

    <T> Optional<T> queryOptional(LockedTable table,
                                  long resourceId,
                                  String sql,
                                  RowMapper<T> rowMapper,
                                  Object... params);

    <T> List<T> queryList(LockedTable table,
                          long resourceId,
                          String sql,
                          RowMapper<T> rowMapper,
                          Object... params);
}
