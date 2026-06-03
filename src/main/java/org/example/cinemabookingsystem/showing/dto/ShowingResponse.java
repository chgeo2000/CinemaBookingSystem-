package org.example.cinemabookingsystem.showing.dto;

import org.example.cinemabookingsystem.showing.Showing;

import java.time.LocalDate;

public record ShowingResponse(
        Long id,
        Long movieId,
        LocalDate screeningDate
) {
    public static ShowingResponse from(Showing showing) {
        return new ShowingResponse(showing.id(), showing.movieId(), showing.screeningDate());
    }
}
