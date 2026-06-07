package org.example.cinemabookingsystem.seat;

/**
 * A physical seat in the cinema hall.
 *
 * <p>Seats are shared across all showings — the same seat row/column exists regardless of
 * what film is playing. Availability per showing is derived at query time by checking whether
 * a {@link org.example.cinemabookingsystem.booking.BookedSeat} record exists for the
 * {@code (showingId, seatId)} pair.
 *
 * <p>{@code rowNumber} and {@code columnNumber} are stored as strings (e.g. {@code "A"}, {@code "1"})
 * to support alphanumeric labelling conventions. {@link SeatType} distinguishes standard from
 * premium seating.
 */
public record Seat(
        Long id,
        String rowNumber,
        String columnNumber,
        SeatType seatType
) {
}
