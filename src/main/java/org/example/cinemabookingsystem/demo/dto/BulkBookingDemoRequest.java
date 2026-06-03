package org.example.cinemabookingsystem.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkBookingDemoRequest(
        @NotNull Long showingId,
        @NotEmpty List<@NotNull Long> seatIds,
        @NotNull Long userId,
        @NotNull Long preBookedSeatId
) {
}
