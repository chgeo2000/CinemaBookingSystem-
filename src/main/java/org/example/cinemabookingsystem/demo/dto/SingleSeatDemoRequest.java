package org.example.cinemabookingsystem.demo.dto;

import jakarta.validation.constraints.NotNull;

public record SingleSeatDemoRequest(
        @NotNull Long showingId,
        @NotNull Long seatId,
        @NotNull Long userIdA,
        @NotNull Long userIdB
) {
}
