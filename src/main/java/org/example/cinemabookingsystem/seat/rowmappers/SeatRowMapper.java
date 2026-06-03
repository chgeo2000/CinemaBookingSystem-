package org.example.cinemabookingsystem.seat.rowmappers;

import org.example.cinemabookingsystem.seat.Seat;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SeatRowMapper implements RowMapper<Seat> {

    @Override
    public Seat mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Seat(
                rs.getLong("id"),
                rs.getString("row_number"),
                rs.getString("column_number"),
                rs.getString("seat_type")
        );
    }
}
