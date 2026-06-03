package org.example.cinemabookingsystem.booking.dto;

import org.example.cinemabookingsystem.booking.BookedSeat;
import org.example.cinemabookingsystem.booking.Booking;

import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id,
        Long userId,
        Long showingId,
        Instant bookingTime,
        String status,
        List<Long> seatIds
) {
    public static BookingResponse from(Booking booking, List<BookedSeat> seats) {
        return new BookingResponse(
                booking.id(),
                booking.userId(),
                booking.showingId(),
                booking.bookingTime(),
                booking.status().name(),
                seats.stream().map(BookedSeat::seatId).toList()
        );
    }
}
