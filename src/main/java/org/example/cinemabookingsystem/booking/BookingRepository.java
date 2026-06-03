package org.example.cinemabookingsystem.booking;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.booking.rowmappers.BookingRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookingRepository {

    private static final String COLUMNS = "id, user_id, showing_id, booking_time, status";

    private static final String SELECT_BY_ID_SQL = "SELECT " + COLUMNS + " FROM booking WHERE id = :id";
    private static final String SELECT_BY_USER_SQL = "SELECT " + COLUMNS + " FROM booking WHERE user_id = :userId ORDER BY booking_time DESC";

    private final JdbcClient jdbcClient;

    public Optional<Booking> findById(Long id) {
        return jdbcClient.sql(SELECT_BY_ID_SQL).param("id", id).query(new BookingRowMapper()).optional();
    }

    public List<Booking> findByUserId(Long userId) {
        return jdbcClient.sql(SELECT_BY_USER_SQL).param("userId", userId).query(new BookingRowMapper()).list();
    }
}
