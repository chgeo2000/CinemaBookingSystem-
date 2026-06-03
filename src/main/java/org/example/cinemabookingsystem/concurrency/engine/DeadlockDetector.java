package org.example.cinemabookingsystem.concurrency.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wraps {@link WaitForGraph} cycle detection with a victim-selection policy.
 *
 * <p>Policy in this implementation: <b>the detector becomes the victim</b> — i.e. the
 * transaction whose wait would close the cycle is the one chosen to abort. This avoids
 * cross-thread abort signalling and produces a correct, easy-to-reason-about engine.
 * A timestamp-based wait-die policy is a natural future extension.
 */
@Component
@RequiredArgsConstructor
public class DeadlockDetector {

    private final WaitForGraph waitForGraph;

    /**
     * Returns the cycle (non-empty) when the supplied waiter would close one; otherwise empty.
     */
    public List<String> detectCycleFor(String waiterTransactionId) {
        return waitForGraph.findCycleStartingFrom(waiterTransactionId);
    }
}
