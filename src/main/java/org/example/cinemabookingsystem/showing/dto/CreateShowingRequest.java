package org.example.cinemabookingsystem.showing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateShowingRequest(
        @NotNull Long movieId,
        @NotNull LocalDate screeningDate
) {
}
