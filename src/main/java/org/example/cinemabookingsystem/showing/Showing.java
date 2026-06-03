package org.example.cinemabookingsystem.showing;

import java.time.LocalDate;

public record Showing(
        Long id,
        Long movieId,
        LocalDate screeningDate
) {
}
