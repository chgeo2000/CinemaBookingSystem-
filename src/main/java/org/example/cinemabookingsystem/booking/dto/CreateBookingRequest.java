package org.example.cinemabookingsystem.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateBookingRequest(
        @NotNull Long showingId,
        @NotEmpty List<@NotNull Long> seatIds
) {
}
