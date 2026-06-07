package org.example.cinemabookingsystem.seat;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.seat.rowmappers.SeatRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SeatRepository {

    private static final String COLUMNS = "id, row_number, column_number, seat_type";

    private static final String SELECT_ALL_SQL = "SELECT " + COLUMNS + " FROM seat ORDER BY id";
    private static final String SELECT_BY_ID_SQL = "SELECT " + COLUMNS + " FROM seat WHERE id = :id";

    private static final String INSERT_SQL = """
            INSERT INTO seat (row_number, column_number, seat_type)
            VALUES (:rowNumber, :columnNumber, :seatType)
            """;

    private final JdbcClient jdbcClient;

    public List<Seat> findAll() {
        return jdbcClient.sql(SELECT_ALL_SQL).query(new SeatRowMapper()).list();
    }

    public Optional<Seat> findById(Long id) {
        return jdbcClient.sql(SELECT_BY_ID_SQL).param("id", id).query(new SeatRowMapper()).optional();
    }

    public Seat save(Seat seat) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(INSERT_SQL)
                .param("rowNumber", seat.rowNumber())
                .param("columnNumber", seat.columnNumber())
                .param("seatType", seat.seatType().name())
                .update(keyHolder);
        return new Seat(extractGeneratedId(keyHolder), seat.rowNumber(), seat.columnNumber(), seat.seatType());
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
