package org.example.cinemabookingsystem.booking;

public record BookedSeat(
        Long id,
        Long bookingId,
        Long showingId,
        Long seatId
) {
}
