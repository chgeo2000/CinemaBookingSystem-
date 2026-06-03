package org.example.cinemabookingsystem.seat.dto;

public record SeatAvailability(
        Long seatId,
        String rowNumber,
        String columnNumber,
        String seatType,
        boolean booked
) {
}
