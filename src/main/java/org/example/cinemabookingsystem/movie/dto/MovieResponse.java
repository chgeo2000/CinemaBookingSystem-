package org.example.cinemabookingsystem.movie.dto;

import org.example.cinemabookingsystem.movie.Movie;

import java.time.LocalDate;

public record MovieResponse(
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
    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.id(), movie.title(), movie.description(), movie.genre(), movie.director(),
                movie.language(), movie.duration(), movie.releaseDate(), movie.rating(), movie.trailerUrl()
        );
    }
}
