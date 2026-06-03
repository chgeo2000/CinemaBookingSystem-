package org.example.cinemabookingsystem.user.rowmappers;

import org.example.cinemabookingsystem.user.CinemaUser;
import org.example.cinemabookingsystem.user.Role;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CinemaUserRowMapper implements RowMapper<CinemaUser> {

    @Override
    public CinemaUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CinemaUser(
                rs.getLong("id"),
                rs.getString("user_name"),
                rs.getString("password"),
                Role.valueOf(rs.getString("role")),
                rs.getString("email"),
                rs.getString("phone_number")
        );
    }
}
