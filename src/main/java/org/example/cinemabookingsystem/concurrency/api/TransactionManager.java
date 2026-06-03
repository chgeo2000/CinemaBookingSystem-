package org.example.cinemabookingsystem.concurrency.api;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sole entry point into the custom Strict 2PL transaction engine.
 *
 * <p>Each {@code execute} call wraps the supplied work in:
 * <ol>
 *   <li>create transaction (status = ACTIVE)</li>
 *   <li>invoke the work lambda — which acquires locks and executes operations via {@link TransactionContext}</li>
 *   <li>on success: release all locks, mark COMMITTED</li>
 *   <li>on exception: replay compensations in reverse, release all locks, mark ABORTED, rethrow</li>
 * </ol>
 *
 * <p>This API replaces {@code @Transactional} for any code path that touches concurrency-sensitive resources.
 */
public interface TransactionManager {

    <T> T execute(Function<TransactionContext, T> work);

    default void execute(Consumer<TransactionContext> work) {
        execute(ctx -> {
            work.accept(ctx);
            return null;
        });
    }
}
