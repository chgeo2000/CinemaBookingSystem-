package org.example.cinemabookingsystem.showing.rowmappers;

import org.example.cinemabookingsystem.showing.Showing;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ShowingRowMapper implements RowMapper<Showing> {

    @Override
    public Showing mapRow(ResultSet rs, int rowNum) throws SQLException {
        Date screeningDate = rs.getDate("screening_date");
        return new Showing(
                rs.getLong("id"),
                rs.getLong("movie_id"),
                screeningDate == null ? null : screeningDate.toLocalDate()
        );
    }
}
