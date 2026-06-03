package org.example.cinemabookingsystem.movie;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.api.TransactionManager;
import org.example.cinemabookingsystem.movie.dto.CreateMovieRequest;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

import static org.example.cinemabookingsystem.concurrency.model.enums.LockedTable.MOVIE;

@Service
@RequiredArgsConstructor
public class MovieService {

    private static final String DELETE_SQL = "DELETE FROM movie WHERE id = ?";
    private static final String COMPENSATE_DELETE_SQL = """
            INSERT INTO movie (id, title, description, genre, director, language,
                               duration, release_date, rating, trailer_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final MovieRepository movieRepository;
    private final TransactionManager transactionManager;

    public List<Movie> findAll() {
        return movieRepository.findAll();
    }

    public Movie findById(Long id) {
        return movieRepository.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
    }

    public Movie create(CreateMovieRequest request) {
        return movieRepository.save(new Movie(
                null, request.title(), request.description(), request.genre(), request.director(),
                request.language(), request.duration(), request.releaseDate(), request.rating(), request.trailerUrl()
        ));
    }

    /**
     * Routed through the 2PL engine. Acquiring EXCLUSIVE on (MOVIE, id) conflicts with any
     * SHARED holders (active booking flows for showings of this movie) — demonstrates the
     * SHARED-vs-EXCLUSIVE compatibility matrix.
     */
    public void delete(Long id) {
        Movie movie = findById(id);
        transactionManager.execute(ctx -> {
            ctx.lockExclusive(MOVIE, id);
            ctx.delete(
                    MOVIE, id,
                    DELETE_SQL, new Object[]{id},
                    COMPENSATE_DELETE_SQL,
                    new Object[]{
                            movie.id(), movie.title(), movie.description(), movie.genre(), movie.director(),
                            movie.language(), movie.duration(),
                            movie.releaseDate() == null ? null : Date.valueOf(movie.releaseDate()),
                            movie.rating(), movie.trailerUrl()
                    }
            );
        });
    }
}
