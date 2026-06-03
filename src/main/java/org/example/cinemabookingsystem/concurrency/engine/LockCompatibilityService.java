package org.example.cinemabookingsystem.concurrency.engine;

import org.example.cinemabookingsystem.concurrency.model.enums.LockType;
import org.springframework.stereotype.Component;

/**
 * Encodes the SHARED/EXCLUSIVE compatibility matrix used by {@link LockManager}.
 *
 * <pre>
 *   held \ requested | SHARED | EXCLUSIVE
 *   -----------------+--------+----------
 *   SHARED           |   ok   |  conflict
 *   EXCLUSIVE        | conflict |  conflict
 * </pre>
 */
@Component
public class LockCompatibilityService {

    public boolean isCompatible(LockType held, LockType requested) {
        return held == LockType.SHARED && requested == LockType.SHARED;
    }

    public boolean conflicts(LockType held, LockType requested) {
        return !isCompatible(held, requested);
    }
}
