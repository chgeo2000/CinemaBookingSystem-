package org.example.cinemabookingsystem.movie.rowmappers;

import org.example.cinemabookingsystem.movie.Movie;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MovieRowMapper implements RowMapper<Movie> {

    @Override
    public Movie mapRow(ResultSet rs, int rowNum) throws SQLException {
        Date releaseDate = rs.getDate("release_date");
        return new Movie(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("genre"),
                rs.getString("director"),
                rs.getString("language"),
                rs.getObject("duration") == null ? null : rs.getInt("duration"),
                releaseDate == null ? null : releaseDate.toLocalDate(),
                rs.getString("rating"),
                rs.getString("trailer_url")
        );
    }
}
