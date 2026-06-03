package org.example.cinemabookingsystem.concurrency.engine;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directed graph of "transaction waits for transaction" edges used to detect deadlocks.
 *
 * <p>An edge {@code waiter -> holder} means {@code waiter} is blocked attempting to acquire
 * a lock currently held in an incompatible mode by {@code holder}. A directed cycle in this
 * graph is a deadlock.
 *
 * <p>All methods are synchronized; the graph is shared state accessed from many transaction
 * threads.
 */
@Component
public class WaitForGraph {

    private final Map<String, Set<String>> outgoingEdges = new HashMap<>();

    public synchronized void addEdge(String waiter, String holder) {
        if (waiter.equals(holder)) {
            return;
        }
        outgoingEdges.computeIfAbsent(waiter, ignored -> new HashSet<>()).add(holder);
    }

    public synchronized void removeAllEdgesFrom(String node) {
        outgoingEdges.remove(node);
    }

    public synchronized void removeNode(String node) {
        outgoingEdges.remove(node);
        outgoingEdges.values().forEach(holders -> holders.remove(node));
    }

    /**
     * Returns the nodes forming a cycle that contains {@code startNode}, in traversal order
     * (with {@code startNode} appearing once at the boundary). Empty list if no cycle.
     */
    public synchronized List<String> findCycleStartingFrom(String startNode) {
        LinkedList<String> path = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        List<String> cycle = new ArrayList<>();
        if (dfsCycle(startNode, path, visited, cycle)) {
            return List.copyOf(cycle);
        }
        return List.of();
    }

    private boolean dfsCycle(String node, LinkedList<String> path, Set<String> visited, List<String> cycleOut) {
        int indexInPath = path.indexOf(node);
        if (indexInPath >= 0) {
            cycleOut.addAll(path.subList(indexInPath, path.size()));
            cycleOut.add(node);
            return true;
        }
        if (!visited.add(node)) {
            return false;
        }
        path.addLast(node);
        Set<String> neighbors = outgoingEdges.getOrDefault(node, Set.of());
        for (String neighbor : neighbors) {
            if (dfsCycle(neighbor, path, visited, cycleOut)) {
                return true;
            }
        }
        path.removeLast();
        return false;
    }

    synchronized Map<String, Set<String>> snapshot() {
        Map<String, Set<String>> copy = new HashMap<>();
        outgoingEdges.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
        return copy;
    }
}
