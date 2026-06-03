package org.example.cinemabookingsystem.booking;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.booking.rowmappers.BookedSeatRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BookedSeatRepository {

    private static final String SELECT_BY_BOOKING_SQL = """
            SELECT id, booking_id, showing_id, seat_id
              FROM booked_seat
             WHERE booking_id = :bookingId
             ORDER BY seat_id
            """;

    private final JdbcClient jdbcClient;

    public List<BookedSeat> findByBookingId(Long bookingId) {
        return jdbcClient.sql(SELECT_BY_BOOKING_SQL)
                .param("bookingId", bookingId)
                .query(new BookedSeatRowMapper())
                .list();
    }
}
