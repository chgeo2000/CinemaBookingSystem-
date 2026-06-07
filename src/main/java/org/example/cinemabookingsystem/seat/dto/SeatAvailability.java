package org.example.cinemabookingsystem.seat.dto;

import org.example.cinemabookingsystem.seat.SeatType;

public record SeatAvailability(
        Long seatId,
        String rowNumber,
        String columnNumber,
        SeatType seatType,
        boolean booked
) {
}
