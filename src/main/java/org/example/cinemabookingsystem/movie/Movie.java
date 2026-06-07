package org.example.cinemabookingsystem.movie;

import java.time.LocalDate;

/**
 * A film available for screening in the cinema.
 *
 * <p>Movies are the top of the domain hierarchy. A movie can have many {@link org.example.cinemabookingsystem.showing.Showing}s
 * scheduled across different dates. Deleting a movie cascades to all its showings, and
 * transitively to the bookings and booked seats made for those showings.
 *
 * <p>{@code duration} is in minutes. {@code rating} holds the age classification
 * (e.g. {@code PG-13}, {@code R}). {@code trailerUrl} is an optional external link.
 */
public record Movie(
        Long id,
        String title,
        String description,
        String genre,
        String director,
        String language,
        Integer duration,
        LocalDate releaseDate,
        String rating,
        String trailerUrl
) {
}
