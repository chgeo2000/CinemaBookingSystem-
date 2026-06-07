package org.example.cinemabookingsystem.concurrency.api;

import java.util.Arrays;
import java.util.List;

/**
 * The undo action for a write, supplied by the caller of {@link TransactionContext#update}
 * and {@link TransactionContext#delete}. Bundles the compensation SQL with its bind parameters
 * so the {@code RollbackManager} can replay it verbatim if the transaction aborts.
 *
 * <p>Use {@link #of(String, Object...)} for the common case:
 * {@code Compensation.of("INSERT INTO booking (...) VALUES (?, ?)", id, status)}.
 *
 * @param sql    the compensation statement, using positional {@code ?} placeholders
 * @param params the bind parameters for {@code sql}, in order; may contain {@code null} elements
 */
public record Compensation(String sql, List<Object> params) {

    public static Compensation of(String sql, Object... params) {
        // Arrays.asList (not List.of) because compensation params may legitimately be null.
        return new Compensation(sql, Arrays.asList(params));
    }
}
