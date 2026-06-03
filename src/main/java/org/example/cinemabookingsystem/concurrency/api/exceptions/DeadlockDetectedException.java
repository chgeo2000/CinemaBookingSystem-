package org.example.cinemabookingsystem.concurrency.api.exceptions;

import java.util.List;

public class DeadlockDetectedException extends TransactionAbortedException {

    private final List<String> cycle;

    public DeadlockDetectedException(String victimTransactionId, List<String> cycle) {
        super(victimTransactionId,
                "Deadlock detected involving transactions: " + cycle + ". Aborting victim: " + victimTransactionId);
        this.cycle = List.copyOf(cycle);
    }

    public List<String> cycle() {
        return cycle;
    }
}
