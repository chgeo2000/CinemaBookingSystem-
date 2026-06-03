package org.example.cinemabookingsystem.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.booking.dto.BookingResponse;
import org.example.cinemabookingsystem.booking.dto.CreateBookingRequest;
import org.example.cinemabookingsystem.user.CinemaUser;
import org.example.cinemabookingsystem.user.CinemaUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final CinemaUserRepository cinemaUserRepository;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal UserDetails principal,
                                                  @Valid @RequestBody CreateBookingRequest request) {
        Long userId = resolveUserId(principal);
        Booking booking = bookingService.createBooking(userId, request.showingId(), request.seatIds());
        List<BookedSeat> seats = bookingService.findSeatsForBooking(booking.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking, seats));
    }

    @GetMapping("/me")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal UserDetails principal) {
        Long userId = resolveUserId(principal);
        return bookingService.findBookingsForUser(userId).stream()
                .map(booking -> BookingResponse.from(booking, bookingService.findSeatsForBooking(booking.id())))
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable Long id) {
        Long userId = resolveUserId(principal);
        bookingService.cancelBooking(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(UserDetails principal) {
        return cinemaUserRepository.findByUserName(principal.getUsername())
                .map(CinemaUser::id)
                .orElseThrow(() -> new UsernameNotFoundException(principal.getUsername()));
    }
}
