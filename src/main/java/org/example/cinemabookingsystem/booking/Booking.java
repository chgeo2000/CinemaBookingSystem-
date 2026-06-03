package org.example.cinemabookingsystem.booking;

import java.time.Instant;

public record Booking(
        Long id,
        Long userId,
        Long showingId,
        Instant bookingTime,
        BookingStatus status
) {
}
