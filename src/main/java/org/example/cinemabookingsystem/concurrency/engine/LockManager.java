package org.example.cinemabookingsystem.concurrency.engine;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.api.exceptions.DeadlockDetectedException;
import org.example.cinemabookingsystem.concurrency.api.exceptions.LockAcquisitionException;
import org.example.cinemabookingsystem.concurrency.model.Lock;
import org.example.cinemabookingsystem.concurrency.model.enums.LockType;
import org.example.cinemabookingsystem.concurrency.model.enums.LockedTable;
import org.example.cinemabookingsystem.concurrency.repository.LockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative lock table for the custom 2PL engine.
 *
 * <p>Holds an in-memory {@code Map<ResourceKey, Lock>} guarded by a single intrinsic monitor.
 * The DB-backed {@link LockRepository} is mirrored alongside for durability and audit but
 * concurrency decisions are made from the in-memory state.
 *
 * <p>Acquire algorithm (per-call, while {@code synchronized}):
 * <ol>
 *   <li>If the resource is unlocked → grant.</li>
 *   <li>If the requester already holds an equal or stronger lock → no-op.</li>
 *   <li>If the requester is the sole SHARED holder and asks for EXCLUSIVE → upgrade.</li>
 *   <li>If the requested mode is compatible (SHARED+SHARED) → add holder.</li>
 *   <li>Otherwise register wait-for edges, run cycle detection. If a cycle is found, the
 *       requester is the victim and a {@link DeadlockDetectedException} is thrown.
 *       Otherwise {@code wait()} on the monitor and re-evaluate when notified.</li>
 * </ol>
 *
 * <p>Single-lock design (one {@code Object} monitor for every resource) trades scalability
 * for thesis-grade clarity and trivially-correct synchronization.
 */
@Component
@RequiredArgsConstructor
public class LockManager {

    private static final Logger log = LoggerFactory.getLogger(LockManager.class);

    private final WaitForGraph waitForGraph;
    private final DeadlockDetector deadlockDetector;
    private final LockCompatibilityService compatibility;
    private final LockRepository lockRepository;

    private final Object globalLock = new Object();
    private final Map<ResourceKey, Lock> heldLocks = new HashMap<>();

    public void acquire(String transactionId, LockedTable table, long resourceId, LockType requestedType) {
        ResourceKey key = new ResourceKey(table, resourceId);
        synchronized (globalLock) {
            while (true) {
                waitForGraph.removeAllEdgesFrom(transactionId);

                Lock existing = heldLocks.get(key);

                if (existing == null) {
                    grantNew(transactionId, table, resourceId, requestedType, key);
                    return;
                }

                if (existing.isHeldBy(transactionId)) {
                    if (existing.type() == LockType.EXCLUSIVE || existing.type() == requestedType) {
                        return;
                    }
                    if (existing.isHeldOnlyBy(transactionId)) {
                        upgradeToExclusive(transactionId, table, resourceId, key);
                        return;
                    }
                } else if (compatibility.isCompatible(existing.type(), requestedType)) {
                    addSharedHolder(transactionId, table, resourceId, requestedType, key, existing);
                    return;
                }

                registerWaitEdges(transactionId, existing);
                List<String> cycle = deadlockDetector.detectCycleFor(transactionId);
                if (!cycle.isEmpty()) {
                    waitForGraph.removeAllEdgesFrom(transactionId);
                    log.warn("Deadlock detected — victim {} cycle {}", transactionId, cycle);
                    throw new DeadlockDetectedException(transactionId, cycle);
                }

                waitForNotification(transactionId, table, resourceId);
            }
        }
    }

    public void releaseAll(String transactionId) {
        synchronized (globalLock) {
            List<ResourceKey> toRemove = new ArrayList<>();
            List<Map.Entry<ResourceKey, Lock>> toUpdate = new ArrayList<>();

            for (Map.Entry<ResourceKey, Lock> entry : heldLocks.entrySet()) {
                Lock lock = entry.getValue();
                if (!lock.isHeldBy(transactionId)) {
                    continue;
                }
                Lock without = lock.withoutHolder(transactionId);
                if (without.transactionHasLockIds().isEmpty()) {
                    toRemove.add(entry.getKey());
                } else {
                    toUpdate.add(Map.entry(entry.getKey(), without));
                }
            }
            toRemove.forEach(heldLocks::remove);
            toUpdate.forEach(e -> heldLocks.put(e.getKey(), e.getValue()));

            waitForGraph.removeNode(transactionId);
            lockRepository.deleteAllForTransaction(transactionId);

            globalLock.notifyAll();
        }
    }

    int heldLockCount() {
        synchronized (globalLock) {
            return heldLocks.size();
        }
    }

    private void grantNew(String transactionId, LockedTable table, long resourceId, LockType type, ResourceKey key) {
        heldLocks.put(key, Lock.newHeldBy(type, table, resourceId, transactionId));
        lockRepository.recordAcquired(transactionId, table, resourceId, type);
    }

    private void upgradeToExclusive(String transactionId, LockedTable table, long resourceId, ResourceKey key) {
        heldLocks.put(key, Lock.newHeldBy(LockType.EXCLUSIVE, table, resourceId, transactionId));
        lockRepository.recordAcquired(transactionId, table, resourceId, LockType.EXCLUSIVE);
    }

    private void addSharedHolder(String transactionId,
                                 LockedTable table,
                                 long resourceId,
                                 LockType requestedType,
                                 ResourceKey key,
                                 Lock existing) {
        heldLocks.put(key, existing.withAdditionalHolder(transactionId));
        lockRepository.recordAcquired(transactionId, table, resourceId, requestedType);
    }

    private void registerWaitEdges(String transactionId, Lock existing) {
        for (String holder : existing.transactionHasLockIds()) {
            if (!holder.equals(transactionId)) {
                waitForGraph.addEdge(transactionId, holder);
            }
        }
    }

    private void waitForNotification(String transactionId, LockedTable table, long resourceId) {
        try {
            globalLock.wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            waitForGraph.removeAllEdgesFrom(transactionId);
            throw new LockAcquisitionException(transactionId,
                    "Interrupted while waiting for lock on " + table + "#" + resourceId, e);
        }
    }

    private record ResourceKey(LockedTable table, long resourceId) {
    }
}
