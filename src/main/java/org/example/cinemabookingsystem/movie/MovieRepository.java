package org.example.cinemabookingsystem.movie;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.movie.rowmappers.MovieRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MovieRepository {

    private static final String COLUMNS =
            "id, title, description, genre, director, language, duration, release_date, rating, trailer_url";

    private static final String SELECT_ALL_SQL = "SELECT " + COLUMNS + " FROM movie ORDER BY id";
    private static final String SELECT_BY_ID_SQL = "SELECT " + COLUMNS + " FROM movie WHERE id = :id";

    private static final String INSERT_SQL = """
            INSERT INTO movie (title, description, genre, director, language,
                               duration, release_date, rating, trailer_url)
            VALUES (:title, :description, :genre, :director, :language,
                    :duration, :releaseDate, :rating, :trailerUrl)
            """;

    private final JdbcClient jdbcClient;

    public List<Movie> findAll() {
        return jdbcClient.sql(SELECT_ALL_SQL).query(new MovieRowMapper()).list();
    }

    public Optional<Movie> findById(Long id) {
        return jdbcClient.sql(SELECT_BY_ID_SQL)
                .param("id", id)
                .query(new MovieRowMapper())
                .optional();
    }

    public Movie save(Movie movie) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(INSERT_SQL)
                .param("title", movie.title())
                .param("description", movie.description())
                .param("genre", movie.genre())
                .param("director", movie.director())
                .param("language", movie.language())
                .param("duration", movie.duration())
                .param("releaseDate", movie.releaseDate() == null ? null : Date.valueOf(movie.releaseDate()))
                .param("rating", movie.rating())
                .param("trailerUrl", movie.trailerUrl())
                .update(keyHolder);

        Long generatedId = extractGeneratedId(keyHolder);
        return new Movie(
                generatedId, movie.title(), movie.description(), movie.genre(), movie.director(),
                movie.language(), movie.duration(), movie.releaseDate(), movie.rating(), movie.trailerUrl()
        );
    }

    private Long extractGeneratedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("INSERT did not return any generated keys");
        }
        Object idValue = keys.get("id");
        if (idValue == null && keys.size() == 1) {
            idValue = keys.values().iterator().next();
        }
        if (idValue instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Could not extract numeric id from generated keys: " + keys);
    }
}
