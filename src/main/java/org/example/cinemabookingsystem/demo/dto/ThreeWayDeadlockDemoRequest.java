package org.example.cinemabookingsystem.demo.dto;

import jakarta.validation.constraints.NotNull;

public record ThreeWayDeadlockDemoRequest(
        @NotNull Long showingId,
        @NotNull Long seat1,
        @NotNull Long seat2,
        @NotNull Long seat3,
        @NotNull Long userIdA,
        @NotNull Long userIdB,
        @NotNull Long userIdC
) {
}
