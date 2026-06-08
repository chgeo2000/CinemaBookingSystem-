package org.example.cinemabookingsystem.user;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.user.rowmappers.CinemaUserRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CinemaUserRepository {

    private static final String SELECT_BY_USER_NAME_SQL = """
            SELECT id, user_name, password, role, email, phone_number
              FROM cinema_user
             WHERE user_name = :userName
            """;

    private static final String INSERT_SQL = """
            INSERT INTO cinema_user (user_name, password, role, email, phone_number)
            VALUES (:userName, :password, :role, :email, :phoneNumber)
            """;

    private final JdbcClient jdbcClient;

    public Optional<CinemaUser> findByUserName(String userName) {
        return jdbcClient.sql(SELECT_BY_USER_NAME_SQL)
                .param("userName", userName)
                .query(new CinemaUserRowMapper())
                .optional();
    }

    public Long save(CinemaUser user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(INSERT_SQL)
                .param("userName", user.userName())
                .param("password", user.password())
                .param("role", user.role().name())
                .param("email", user.email())
                .param("phoneNumber", user.phoneNumber())
                .update(keyHolder);
        return extractGeneratedId(keyHolder);
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
