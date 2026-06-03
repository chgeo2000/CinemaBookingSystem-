package org.example.cinemabookingsystem.booking;

public class BookingForbiddenException extends RuntimeException {

    public BookingForbiddenException() {
        super("You cannot modify a booking that does not belong to you");
    }
}
