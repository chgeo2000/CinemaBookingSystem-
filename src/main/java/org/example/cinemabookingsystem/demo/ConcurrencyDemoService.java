package org.example.cinemabookingsystem.demo;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.booking.Booking;
import org.example.cinemabookingsystem.booking.BookingService;
import org.example.cinemabookingsystem.demo.dto.BulkBookingDemoRequest;
import org.example.cinemabookingsystem.demo.dto.DemoReport;
import org.example.cinemabookingsystem.demo.dto.DemoTransactionResult;
import org.example.cinemabookingsystem.demo.dto.SingleSeatDemoRequest;
import org.example.cinemabookingsystem.demo.dto.ThreeWayDeadlockDemoRequest;
import org.example.cinemabookingsystem.demo.dto.TwoSeatDeadlockDemoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Runs the four prescribed concurrency demos against the live booking service. Each demo
 * spawns N virtual threads, fires them in parallel through the 2PL engine, and collects
 * per-transaction outcomes (COMMITTED / ABORTED + cause).
 *
 * <p>Demos use the lock-order-explicit entry point {@code bookSeatsWithLockOrder} so the
 * harness can construct cross-thread orderings that deliberately deadlock. Production
 * traffic going through {@code createBooking} cannot deadlock in the same way.
 */
@Service
@RequiredArgsConstructor
public class ConcurrencyDemoService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyDemoService.class);

    private final BookingService bookingService;

    /**
     * Scenario A1 — single-seat conflict. Two users race to book the same seat for the same
     * showing. Expected: 1 commit, 1 abort ({@code SeatAlreadyBookedException}).
     */
    public DemoReport singleSeatConflict(SingleSeatDemoRequest req) {
        List<DemoTransactionResult> results = runInParallel(List.of(
                () -> attempt("user-A", () -> bookingService.bookSeatsWithLockOrder(
                        req.userIdA(), req.showingId(), List.of(req.seatId()))),
                () -> attempt("user-B", () -> bookingService.bookSeatsWithLockOrder(
                        req.userIdB(), req.showingId(), List.of(req.seatId())))
        ));
        return DemoReport.summarise("single-seat-conflict", results);
    }

    /**
     * Scenario A2 — classic 2-way deadlock. A locks seat 1 then waits for seat 2; B locks
     * seat 2 then waits for seat 1. Expected: 1 commit, 1 abort ({@code DeadlockDetectedException}).
     */
    public DemoReport classicDeadlock(TwoSeatDeadlockDemoRequest req) {
        List<DemoTransactionResult> results = runInParallel(List.of(
                () -> attempt("user-A", () -> bookingService.bookSeatsWithLockOrder(
                        req.userIdA(), req.showingId(), List.of(req.seatA(), req.seatB()))),
                () -> attempt("user-B", () -> bookingService.bookSeatsWithLockOrder(
                        req.userIdB(), req.showingId(), List.of(req.seatB(), req.seatA())))
        ));
        return DemoReport.summarise("classic-deadlock", results);
    }

    /**
     * Scenario B — 3-way deadlock cycle (A→B→C→A). Proves the WaitForGraph DFS handles
     * cycles of length &gt; 2. Expected: 2 commits + 1 abort ({@code DeadlockDetectedException})
     * for whichever transaction closes the cycle.
     */
    public DemoReport threeWayDeadlock(ThreeWayDeadlockDemoRequest req) {
        List<DemoTransactionResult> results = runInParallel(List.of(
                () -> attempt("user-A", () -> bookingService.bookSeatsWithLockOrder(
                        req.userIdA(), req.showingId(), List.of(req.seat1(), req.seat2()))),
                () -> attempt("user-B", () -> bookingService.bookSeatsWithLockOrder(
                        req.userIdB(), req.showingId(), List.of(req.seat2(), req.seat3()))),
                () -> attempt("user-C", () -> bookingService.bookSeatsWithLockOrder(
                        req.userIdC(), req.showingId(), List.of(req.seat3(), req.seat1())))
        ));
        return DemoReport.summarise("three-way-deadlock", results);
    }

    /**
     * Scenario C — bulk-booking atomicity. One user books N seats; one of those seats was
     * pre-booked by another user, so the bulk booking must roll back ALL of its already-inserted
     * seats AND the parent booking row. Expected: 1 abort, no orphan rows.
     */
    public DemoReport bulkBookingAtomicity(BulkBookingDemoRequest req) {
        // Pre-book the conflicting seat in its own transaction.
        bookingService.createBooking(req.userId(), req.showingId(), List.of(req.preBookedSeatId()));

        // Now the bulk booking tries to take {pre-booked + others} and must roll back wholesale.
        List<Long> requestedSeats = new ArrayList<>(req.seatIds());
        if (!requestedSeats.contains(req.preBookedSeatId())) {
            requestedSeats.add(req.preBookedSeatId());
        }

        List<DemoTransactionResult> results = runInParallel(List.of(
                () -> attempt("bulk-booker", () -> bookingService.createBooking(
                        req.userId(), req.showingId(), requestedSeats))
        ));
        return DemoReport.summarise("bulk-booking-atomicity", results);
    }

    private List<DemoTransactionResult> runInParallel(List<Supplier<DemoTransactionResult>> tasks) {
        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
            List<CompletableFuture<DemoTransactionResult>> futures = tasks.stream()
                    .map(t -> CompletableFuture.supplyAsync(t, executor))
                    .toList();
            List<DemoTransactionResult> results = new ArrayList<>();
            for (CompletableFuture<DemoTransactionResult> f : futures) {
                try {
                    results.add(f.get(30, TimeUnit.SECONDS));
                } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
                    Thread.currentThread().interrupt();
                    log.error("Demo task failed to complete in time", e);
                    results.add(DemoTransactionResult.aborted("unknown", -1, e));
                }
            }
            return results;
        }
    }

    private DemoTransactionResult attempt(String label, Supplier<Booking> work) {
        long start = System.nanoTime();
        try {
            Booking booking = work.get();
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            return DemoTransactionResult.committed(label, durationMs, booking.id());
        } catch (RuntimeException e) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            return DemoTransactionResult.aborted(label, durationMs, e);
        }
    }
}
