package org.example.cinemabookingsystem.showing;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.showing.dto.CreateShowingRequest;
import org.example.cinemabookingsystem.showing.dto.ShowingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/showings")
@RequiredArgsConstructor
public class ShowingController {

    private final ShowingService showingService;

    @GetMapping
    public List<ShowingResponse> list(@RequestParam(required = false) Long movieId) {
        List<Showing> showings = movieId == null ? showingService.findAll() : showingService.findByMovieId(movieId);
        return showings.stream().map(ShowingResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ShowingResponse details(@PathVariable Long id) {
        return ShowingResponse.from(showingService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowingResponse> create(@Valid @RequestBody CreateShowingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ShowingResponse.from(showingService.create(request)));
    }
}
