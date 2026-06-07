package org.example.cinemabookingsystem.seat.rowmappers;

import org.example.cinemabookingsystem.seat.dto.SeatAvailability;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SeatAvailabilityRowMapper implements RowMapper<SeatAvailability> {

    @Override
    public SeatAvailability mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SeatAvailability(
                rs.getLong("seat_id"),
                rs.getString("row_number"),
                rs.getString("column_number"),
                rs.getString("seat_type"),
                rs.getBoolean("booked")
        );
    }
}
