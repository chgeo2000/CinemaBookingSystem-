package org.example.cinemabookingsystem.booking.rowmappers;

import org.example.cinemabookingsystem.booking.Booking;
import org.example.cinemabookingsystem.booking.BookingStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class BookingRowMapper implements RowMapper<Booking> {

    @Override
    public Booking mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp bookingTime = rs.getTimestamp("booking_time");
        return new Booking(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("showing_id"),
                bookingTime == null ? null : bookingTime.toInstant(),
                BookingStatus.valueOf(rs.getString("status"))
        );
    }
}
