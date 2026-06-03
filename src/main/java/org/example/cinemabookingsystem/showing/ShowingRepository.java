package org.example.cinemabookingsystem.showing;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.showing.rowmappers.ShowingRowMapper;
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
public class ShowingRepository {

    private static final String COLUMNS = "id, movie_id, screening_date";

    private static final String SELECT_BY_ID_SQL = "SELECT " + COLUMNS + " FROM showing WHERE id = :id";
    private static final String SELECT_BY_MOVIE_SQL = "SELECT " + COLUMNS + " FROM showing WHERE movie_id = :movieId ORDER BY screening_date, id";
    private static final String SELECT_ALL_SQL = "SELECT " + COLUMNS + " FROM showing ORDER BY screening_date, id";

    private static final String INSERT_SQL = """
            INSERT INTO showing (movie_id, screening_date)
            VALUES (:movieId, :screeningDate)
            """;

    private final JdbcClient jdbcClient;

    public Optional<Showing> findById(Long id) {
        return jdbcClient.sql(SELECT_BY_ID_SQL).param("id", id).query(new ShowingRowMapper()).optional();
    }

    public List<Showing> findByMovieId(Long movieId) {
        return jdbcClient.sql(SELECT_BY_MOVIE_SQL).param("movieId", movieId).query(new ShowingRowMapper()).list();
    }

    public List<Showing> findAll() {
        return jdbcClient.sql(SELECT_ALL_SQL).query(new ShowingRowMapper()).list();
    }

    public Showing save(Showing showing) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(INSERT_SQL)
                .param("movieId", showing.movieId())
                .param("screeningDate", showing.screeningDate() == null ? null : Date.valueOf(showing.screeningDate()))
                .update(keyHolder);
        return new Showing(extractGeneratedId(keyHolder), showing.movieId(), showing.screeningDate());
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
