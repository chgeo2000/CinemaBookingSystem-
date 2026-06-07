package org.example.cinemabookingsystem.booking;

/**
 * A single seat claimed within a {@link Booking}.
 *
 * <p>A booked seat is the junction between a booking, a showing, and a physical seat.
 * Storing {@code showingId} directly (in addition to the FK through {@code bookingId → showing})
 * allows an efficient existence check — {@code SELECT EXISTS ... WHERE showing_id = ? AND seat_id = ?} —
 * without a join, which is the hot path executed under an exclusive lock on every booking attempt.
 *
 * <p>A {@code UNIQUE (showing_id, seat_id)} constraint at the schema level acts as a last-resort
 * safety net, guaranteeing single-occupancy even if a lock were somehow bypassed. In normal
 * operation the 2PL engine enforces this before any insert reaches the database.
 */
public record BookedSeat(
        Long id,
        Long bookingId,
        Long showingId,
        Long seatId
) {
}
