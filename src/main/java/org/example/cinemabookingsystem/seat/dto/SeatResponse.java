package org.example.cinemabookingsystem.seat.dto;

import org.example.cinemabookingsystem.seat.Seat;
import org.example.cinemabookingsystem.seat.SeatType;

public record SeatResponse(
        Long id,
        String rowNumber,
        String columnNumber,
        SeatType seatType
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.id(), seat.rowNumber(), seat.columnNumber(), seat.seatType());
    }
}
