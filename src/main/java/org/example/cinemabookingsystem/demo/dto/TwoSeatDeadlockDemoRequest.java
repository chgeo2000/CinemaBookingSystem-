package org.example.cinemabookingsystem.demo.dto;

import jakarta.validation.constraints.NotNull;

public record TwoSeatDeadlockDemoRequest(
        @NotNull Long showingId,
        @NotNull Long seatA,
        @NotNull Long seatB,
        @NotNull Long userIdA,
        @NotNull Long userIdB
) {
}
