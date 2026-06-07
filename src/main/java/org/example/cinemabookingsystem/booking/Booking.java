package org.example.cinemabookingsystem.booking;

import java.time.Instant;

/**
 * A reservation made by a {@link org.example.cinemabookingsystem.user.CinemaUser} for a
 * specific {@link org.example.cinemabookingsystem.showing.Showing}.
 *
 * <p>A booking is the parent record of the reservation — it records who booked, for which
 * showing, and when. The individual seats claimed are stored as
 * {@link BookedSeat} records that reference this booking. A booking can cover one or more
 * seats in a single transaction.
 *
 * <p>Creating a booking is the primary operation exercised by the custom Strict 2PL engine:
 * the engine acquires exclusive locks per seat and shared locks on the parent showing and movie
 * before inserting any rows, guaranteeing that two concurrent users cannot claim the same seat.
 */
public record Booking(
        Long id,
        Long userId,
        Long showingId,
        Instant bookingTime,
        BookingStatus status
) {
}
