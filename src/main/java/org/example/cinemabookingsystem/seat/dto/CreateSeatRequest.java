package org.example.cinemabookingsystem.seat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.cinemabookingsystem.seat.SeatType;

public record CreateSeatRequest(
        @NotBlank @Size(max = 5) String rowNumber,
        @NotBlank @Size(max = 5) String columnNumber,
        @NotNull SeatType seatType
) {
}
