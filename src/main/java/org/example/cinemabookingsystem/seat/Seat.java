package org.example.cinemabookingsystem.seat;

public record Seat(
        Long id,
        String rowNumber,
        String columnNumber,
        String seatType
) {
}
