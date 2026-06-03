package org.example.cinemabookingsystem.seat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSeatRequest(
        @NotBlank @Size(max = 5) String rowNumber,
        @NotBlank @Size(max = 5) String columnNumber,
        @Size(max = 30) String seatType
) {
}
