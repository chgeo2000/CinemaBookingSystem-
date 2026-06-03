package org.example.cinemabookingsystem.booking;

public class SeatAlreadyBookedException extends RuntimeException {

    public SeatAlreadyBookedException(Long showingId, Long seatId) {
        super("Seat already booked for showing " + showingId + ": seat " + seatId);
    }
}
