package org.example.cinemabookingsystem.common;

import org.example.cinemabookingsystem.booking.BookingForbiddenException;
import org.example.cinemabookingsystem.booking.BookingNotFoundException;
import org.example.cinemabookingsystem.booking.SeatAlreadyBookedException;
import org.example.cinemabookingsystem.concurrency.api.exceptions.DeadlockDetectedException;
import org.example.cinemabookingsystem.concurrency.api.exceptions.LockAcquisitionException;
import org.example.cinemabookingsystem.concurrency.api.exceptions.TransactionAbortedException;
import org.example.cinemabookingsystem.movie.MovieNotFoundException;
import org.example.cinemabookingsystem.showing.ShowingNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler({MovieNotFoundException.class, ShowingNotFoundException.class, BookingNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(BookingForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(BookingForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(SeatAlreadyBookedException.class)
    public ResponseEntity<Map<String, String>> handleSeatTaken(SeatAlreadyBookedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DeadlockDetectedException.class)
    public ResponseEntity<Map<String, Object>> handleDeadlock(DeadlockDetectedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Deadlock detected — your transaction was chosen as the victim. Please retry.",
                "transactionId", e.transactionId(),
                "cycle", e.cycle()
        ));
    }

    @ExceptionHandler({LockAcquisitionException.class, TransactionAbortedException.class})
    public ResponseEntity<Map<String, String>> handleTransactionAbort(TransactionAbortedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Transaction aborted: " + e.getMessage(),
                "transactionId", e.transactionId()
        ));
    }
}
