package org.example.cinemabookingsystem.concurrency.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.example.cinemabookingsystem.concurrency.model.enums.OperationType;

import java.time.Instant;

public record Operation(
        Long id,
        String transactionId,
        int sequenceNumber,
        OperationType type,
        LockedTable table,
        Long resourceId,
        String compensationSql,
        JsonNode compensationParams,
        Instant executedAt
) {
    public boolean hasCompensation() {
        return StringUtils.isNotBlank(compensationSql);
    }
}
