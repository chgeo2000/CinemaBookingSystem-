package org.example.cinemabookingsystem.concurrency.model.rowmappers;

import org.example.cinemabookingsystem.concurrency.model.Transaction;
import org.example.cinemabookingsystem.concurrency.model.enums.TransactionStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TransactionRowMapper implements RowMapper<Transaction> {

    @Override
    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp endedAt = rs.getTimestamp("ended_at");
        return new Transaction(
                rs.getString("id"),
                rs.getTimestamp("started_at").toInstant(),
                endedAt == null ? null : endedAt.toInstant(),
                TransactionStatus.valueOf(rs.getString("status"))
        );
    }
}
