package org.example.cinemabookingsystem.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.booking.dto.BookingResponse;
import org.example.cinemabookingsystem.booking.dto.CreateBookingRequest;
import org.example.cinemabookingsystem.user.CinemaUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal CinemaUserPrincipal principal,
                                                  @Valid @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(principal.id(), request.showingId(), request.seatIds());
        List<BookedSeat> seats = bookingService.findSeatsForBooking(booking.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking, seats));
    }

    @GetMapping("/me")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal CinemaUserPrincipal principal) {
        return bookingService.findBookingsForUser(principal.id()).stream()
                .map(booking -> BookingResponse.from(booking, bookingService.findSeatsForBooking(booking.id())))
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal CinemaUserPrincipal principal,
                                       @PathVariable Long id) {
        bookingService.cancelBooking(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
