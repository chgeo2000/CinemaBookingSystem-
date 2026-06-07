package org.example.cinemabookingsystem.showing;

import java.time.LocalDate;

/**
 * A scheduled screening of a {@link org.example.cinemabookingsystem.movie.Movie} on a specific date.
 *
 * <p>A showing is the unit users browse when picking what to watch and when. Each showing
 * has its own seat availability — the same physical {@link org.example.cinemabookingsystem.seat.Seat}
 * can be booked independently across different showings. Deleting a showing cascades to
 * all {@link org.example.cinemabookingsystem.booking.Booking}s and
 * {@link org.example.cinemabookingsystem.booking.BookedSeat}s made for it.
 */
public record Showing(
        Long id,
        Long movieId,
        LocalDate screeningDate
) {
}
