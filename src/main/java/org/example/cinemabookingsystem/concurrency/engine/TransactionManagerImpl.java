package org.example.cinemabookingsystem.concurrency.engine;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.api.TransactionContext;
import org.example.cinemabookingsystem.concurrency.api.TransactionManager;
import org.example.cinemabookingsystem.concurrency.api.exceptions.TransactionAbortedException;
import org.example.cinemabookingsystem.concurrency.model.Transaction;
import org.example.cinemabookingsystem.concurrency.model.enums.TransactionStatus;
import org.example.cinemabookingsystem.concurrency.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.function.Function;

/**
 * Orchestrates the lifecycle of a single transaction in the custom 2PL engine.
 *
 * <p>Each {@link #execute} call:
 * <pre>
 *  1. creates a Transaction (status=ACTIVE) and persists it
 *  2. runs the user lambda with a fresh {@link TransactionContext}
 *  3. on success: releases all locks, marks COMMITTED
 *  4. on TransactionAbortedException (deadlock, lock failure):
 *       runs RollbackManager, releases locks, marks ABORTED, rethrows
 *  5. on any other exception:
 *       runs RollbackManager, releases locks, marks ERROR, rethrows
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class TransactionManagerImpl implements TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(TransactionManagerImpl.class);

    private final TransactionRepository transactionRepository;
    private final LockManager lockManager;
    private final OperationExecutor operationExecutor;
    private final RollbackManager rollbackManager;

    @Override
    public <T> T execute(Function<TransactionContext, T> work) {
        Transaction transaction = Transaction.newActive();
        transactionRepository.save(transaction);
        log.debug("Started transaction {}", transaction.id());

        TransactionContextImpl ctx = new TransactionContextImpl(transaction.id(), lockManager, operationExecutor);

        try {
            T result = work.apply(ctx);
            commit(transaction.id());
            return result;
        } catch (TransactionAbortedException e) {
            abort(transaction.id(), e);
            throw e;
        } catch (RuntimeException e) {
            error(transaction.id(), e);
            throw e;
        }
    }

    private void commit(String transactionId) {
        lockManager.releaseAll(transactionId);
        transactionRepository.updateStatus(transactionId, TransactionStatus.COMMITTED, Instant.now());
        log.debug("Committed transaction {}", transactionId);
    }

    private void abort(String transactionId, TransactionAbortedException cause) {
        log.info("Aborting transaction {}: {}", transactionId, cause.getMessage());
        try {
            rollbackManager.rollback(transactionId);
        } finally {
            lockManager.releaseAll(transactionId);
            transactionRepository.updateStatus(transactionId, TransactionStatus.ABORTED, Instant.now());
        }
    }

    private void error(String transactionId, Throwable cause) {
        log.error("Transaction {} failed with unexpected error: {}", transactionId, cause.getMessage(), cause);
        try {
            rollbackManager.rollback(transactionId);
        } finally {
            lockManager.releaseAll(transactionId);
            transactionRepository.updateStatus(transactionId, TransactionStatus.ERROR, Instant.now());
        }
    }
}
