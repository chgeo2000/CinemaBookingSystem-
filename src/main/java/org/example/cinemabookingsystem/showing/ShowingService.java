package org.example.cinemabookingsystem.showing;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.movie.MovieService;
import org.example.cinemabookingsystem.showing.dto.CreateShowingRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowingService {

    private final ShowingRepository showingRepository;
    private final MovieService movieService;

    public List<Showing> findAll() {
        return showingRepository.findAll();
    }

    public List<Showing> findByMovieId(Long movieId) {
        return showingRepository.findByMovieId(movieId);
    }

    public Showing findById(Long id) {
        return showingRepository.findById(id).orElseThrow(() -> new ShowingNotFoundException(id));
    }

    public Showing create(CreateShowingRequest request) {
        movieService.findById(request.movieId());
        return showingRepository.save(new Showing(null, request.movieId(), request.screeningDate()));
    }
}
