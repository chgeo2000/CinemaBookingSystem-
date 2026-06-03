package org.example.cinemabookingsystem.seat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.seat.dto.CreateSeatRequest;
import org.example.cinemabookingsystem.seat.dto.SeatAvailability;
import org.example.cinemabookingsystem.seat.dto.SeatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/api/seats")
    public List<SeatResponse> list() {
        return seatService.findAll().stream().map(SeatResponse::from).toList();
    }

    @GetMapping("/api/showings/{id}/seats")
    public List<SeatAvailability> availabilityForShowing(@PathVariable("id") Long showingId) {
        return seatService.availabilityForShowing(showingId);
    }

    @PostMapping("/api/seats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeatResponse> create(@Valid @RequestBody CreateSeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SeatResponse.from(seatService.create(request)));
    }
}
