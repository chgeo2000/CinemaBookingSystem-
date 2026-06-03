package org.example.cinemabookingsystem.booking.rowmappers;

import org.example.cinemabookingsystem.booking.BookedSeat;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BookedSeatRowMapper implements RowMapper<BookedSeat> {

    @Override
    public BookedSeat mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new BookedSeat(
                rs.getLong("id"),
                rs.getLong("booking_id"),
                rs.getLong("showing_id"),
                rs.getLong("seat_id")
        );
    }
}
