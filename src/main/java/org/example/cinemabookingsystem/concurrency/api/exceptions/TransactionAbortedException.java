package org.example.cinemabookingsystem.concurrency.api.exceptions;

public class TransactionAbortedException extends RuntimeException {

    private final String transactionId;

    public TransactionAbortedException(String transactionId, String message) {
        super(message);
        this.transactionId = transactionId;
    }

    public TransactionAbortedException(String transactionId, String message, Throwable cause) {
        super(message, cause);
        this.transactionId = transactionId;
    }

    public String transactionId() {
        return transactionId;
    }
}
