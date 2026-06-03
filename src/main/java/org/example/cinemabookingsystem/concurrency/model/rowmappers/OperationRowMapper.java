package org.example.cinemabookingsystem.concurrency.model.rowmappers;

import org.example.cinemabookingsystem.concurrency.model.Operation;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.example.cinemabookingsystem.concurrency.model.enums.OperationType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.example.cinemabookingsystem.utils.JsonParser.parseJson;

public class OperationRowMapper implements RowMapper<Operation> {

    @Override
    public Operation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Operation(
                rs.getLong("id"),
                rs.getString("transaction_id"),
                rs.getInt("sequence_number"),
                OperationType.valueOf(rs.getString("operation_type")),
                LockedTable.valueOf(rs.getString("table_name")),
                rs.getObject("resource_id") == null ? null : rs.getLong("resource_id"),
                rs.getString("compensation_sql"),
                parseJson(rs.getString("compensation_params_json")),
                rs.getTimestamp("executed_at").toInstant()
        );
    }
}
