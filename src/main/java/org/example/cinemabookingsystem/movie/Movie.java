package org.example.cinemabookingsystem.movie;

import java.time.LocalDate;

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
