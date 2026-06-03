package org.example.cinemabookingsystem.concurrency.api.exceptions;

public class LockAcquisitionException extends TransactionAbortedException {

    public LockAcquisitionException(String transactionId, String message) {
        super(transactionId, message);
    }

    public LockAcquisitionException(String transactionId, String message, Throwable cause) {
        super(transactionId, message, cause);
    }
}
