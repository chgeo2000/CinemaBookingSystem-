package org.example.cinemabookingsystem.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateMovieRequest(
        @NotBlank @Size(max = 50) String title,
        @Size(max = 1000) String description,
        @Size(max = 30) String genre,
        @Size(max = 30) String director,
        @Size(max = 30) String language,
        @Positive Integer duration,
        LocalDate releaseDate,
        @Size(max = 50) String rating,
        @Size(max = 200) String trailerUrl
) {
}
