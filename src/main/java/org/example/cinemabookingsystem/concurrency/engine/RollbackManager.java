package org.example.cinemabookingsystem.concurrency.engine;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.model.Operation;
import org.example.cinemabookingsystem.concurrency.repository.OperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.example.cinemabookingsystem.utils.JsonParser.toJavaArray;

/**
 * Replays each logged operation's compensation SQL in reverse {@code sequence_number} order.
 *
 * <p>If an individual compensation fails (e.g. the row already gone) the failure is logged
 * and rollback continues with the remaining operations — partial rollback is preferable to
 * abandoning the entire compensation sequence.
 */
@Component
@RequiredArgsConstructor
public class RollbackManager {

    private static final Logger log = LoggerFactory.getLogger(RollbackManager.class);

    private final OperationRepository operationRepository;
    private final JdbcClient jdbcClient;

    public void rollback(String transactionId) {
        List<Operation> operations = operationRepository.findForRollback(transactionId);
        log.info("Rolling back transaction {} — {} compensations to replay", transactionId, operations.size());

        for (Operation op : operations) {
            if (!op.hasCompensation()) {
                continue;
            }
            try {
                executeCompensation(op);
            } catch (RuntimeException e) {
                log.error("Compensation failed for transaction {} operation sequence {}: {}",
                        transactionId, op.sequenceNumber(), e.getMessage(), e);
            }
        }
    }

    private void executeCompensation(Operation op) {
        Object[] params = toJavaArray(op.compensationParams());
        jdbcClient.sql(op.compensationSql())
                .params(params)
                .update();
    }
}
