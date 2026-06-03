package org.example.cinemabookingsystem.demo.dto;

public record DemoTransactionResult(
        String label,
        String outcome,
        String errorType,
        String errorMessage,
        long durationMillis,
        Long bookingId
) {
    public static DemoTransactionResult committed(String label, long durationMillis, Long bookingId) {
        return new DemoTransactionResult(label, "COMMITTED", null, null, durationMillis, bookingId);
    }

    public static DemoTransactionResult aborted(String label, long durationMillis, Throwable error) {
        return new DemoTransactionResult(
                label, "ABORTED",
                error.getClass().getSimpleName(),
                error.getMessage(),
                durationMillis,
                null
        );
    }
}
