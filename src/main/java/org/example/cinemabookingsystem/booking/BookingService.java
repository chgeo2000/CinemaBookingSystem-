package org.example.cinemabookingsystem.booking;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.concurrency.api.Compensation;
import org.example.cinemabookingsystem.concurrency.api.TransactionContext;
import org.example.cinemabookingsystem.concurrency.api.TransactionManager;
import org.example.cinemabookingsystem.showing.Showing;
import org.example.cinemabookingsystem.showing.ShowingRepository;
import org.example.cinemabookingsystem.showing.ShowingNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.example.cinemabookingsystem.booking.SeatLockKey.encode;
import static org.example.cinemabookingsystem.concurrency.model.enums.LockedTable.BOOKED_SEAT;
import static org.example.cinemabookingsystem.concurrency.model.enums.LockedTable.BOOKING;
import static org.example.cinemabookingsystem.concurrency.model.enums.LockedTable.MOVIE;
import static org.example.cinemabookingsystem.concurrency.model.enums.LockedTable.SHOWING;

/**
 * Books and cancels seats through the custom 2PL engine.
 *
 * <p>Every write path runs inside {@link TransactionManager#execute}, so every operation
 * (a) acquires its locks before executing, (b) is logged for compensation-based rollback,
 * and (c) holds those locks until commit/abort (Strict 2PL).
 *
 * <p>The booking flow acquires SHARED locks on the parent {@code (MOVIE, movieId)} and
 * {@code (SHOWING, showingId)} so an admin trying to {@code DELETE} either of those will
 * wait behind active bookings (scenario D). Per-seat conflicts are resolved on a composite
 * {@code (BOOKED_SEAT, encode(showingId, seatId))} EXCLUSIVE lock.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String INSERT_BOOKING_SQL = """
            INSERT INTO booking (user_id, showing_id, booking_time, status)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_BOOKED_SEAT_SQL = """
            INSERT INTO booked_seat (booking_id, showing_id, seat_id)
            VALUES (?, ?, ?)
            """;

    private static final String SEAT_TAKEN_CHECK_SQL =
            "SELECT EXISTS(SELECT 1 FROM booked_seat WHERE showing_id = ? AND seat_id = ?)";

    private static final String DELETE_BOOKING_SQL = "DELETE FROM booking WHERE id = ?";
    private static final String COMPENSATE_DELETE_BOOKING_SQL = """
            INSERT INTO booking (id, user_id, showing_id, booking_time, status)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String DELETE_BOOKED_SEAT_SQL = "DELETE FROM booked_seat WHERE id = ?";
    private static final String COMPENSATE_DELETE_BOOKED_SEAT_SQL = """
            INSERT INTO booked_seat (id, booking_id, showing_id, seat_id)
            VALUES (?, ?, ?, ?)
            """;

    private final TransactionManager transactionManager;
    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final ShowingRepository showingRepository;

    /**
     * Public booking entry point. Sorts seat IDs in ascending order to give every concurrent
     * booking the same lock-acquisition order — eliminating spontaneous deadlocks under
     * non-malicious traffic. Demo paths bypass this by calling {@link #bookSeatsWithLockOrder}.
     */
    public Booking createBooking(Long userId, Long showingId, List<Long> seatIds) {
        List<Long> sorted = seatIds.stream().distinct().sorted().toList();
        return bookSeatsWithLockOrder(userId, showingId, sorted);
    }

    /**
     * Books the supplied seats acquiring locks in <i>exactly</i> the supplied order.
     * Public so the demo harness can construct cross-thread orderings that deadlock.
     * Production callers should use {@link #createBooking} instead.
     */
    public Booking bookSeatsWithLockOrder(Long userId, Long showingId, List<Long> seatIdsInLockOrder) {
        Showing showing = showingRepository.findById(showingId)
                .orElseThrow(() -> new ShowingNotFoundException(showingId));

        return transactionManager.execute(ctx -> {
            ctx.lockShared(MOVIE, showing.movieId());
            ctx.lockShared(SHOWING, showingId);

            for (Long seatId : seatIdsInLockOrder) {
                ctx.lockExclusive(BOOKED_SEAT, encode(showingId, seatId));
                if (isSeatAlreadyBooked(ctx, showingId, seatId)) {
                    throw new SeatAlreadyBookedException(showingId, seatId);
                }
            }

            long bookingId = insertBooking(ctx, userId, showingId);
            for (Long seatId : seatIdsInLockOrder) {
                ctx.insert(BOOKED_SEAT, INSERT_BOOKED_SEAT_SQL, bookingId, showingId, seatId);
            }
            return new Booking(bookingId, userId, showingId, Instant.now(), BookingStatus.CONFIRMED);
        });
    }

    public void cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.userId().equals(userId)) {
            throw new BookingForbiddenException();
        }

        List<BookedSeat> bookedSeats = bookedSeatRepository.findByBookingId(bookingId);

        transactionManager.execute(ctx -> {
            ctx.lockExclusive(BOOKING, bookingId);

            // Sort by composite lock key so concurrent cancels never deadlock.
            List<BookedSeat> sorted = bookedSeats.stream()
                    .sorted(Comparator.comparingLong(bs -> encode(bs.showingId(), bs.seatId())))
                    .toList();

            for (BookedSeat bs : sorted) {
                ctx.lockExclusive(BOOKED_SEAT, encode(bs.showingId(), bs.seatId()));
                ctx.delete(
                        BOOKED_SEAT, bs.id(),
                        DELETE_BOOKED_SEAT_SQL,
                        Compensation.of(COMPENSATE_DELETE_BOOKED_SEAT_SQL,
                                bs.id(), bs.bookingId(), bs.showingId(), bs.seatId()),
                        bs.id()
                );
            }

            ctx.delete(
                    BOOKING, bookingId,
                    DELETE_BOOKING_SQL,
                    Compensation.of(COMPENSATE_DELETE_BOOKING_SQL,
                            booking.id(), booking.userId(), booking.showingId(),
                            booking.bookingTime() == null ? null : Timestamp.from(booking.bookingTime()),
                            booking.status().name()),
                    bookingId
            );
        });
    }

    public List<Booking> findBookingsForUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<BookedSeat> findSeatsForBooking(Long bookingId) {
        return bookedSeatRepository.findByBookingId(bookingId);
    }

    private boolean isSeatAlreadyBooked(TransactionContext ctx, Long showingId, Long seatId) {
        return ctx.exists(BOOKED_SEAT, encode(showingId, seatId), SEAT_TAKEN_CHECK_SQL, showingId, seatId);
    }

    private long insertBooking(TransactionContext ctx, Long userId, Long showingId) {
        return ctx.insert(
                BOOKING,
                INSERT_BOOKING_SQL,
                userId, showingId, Timestamp.from(Instant.now()), BookingStatus.CONFIRMED.name()
        );
    }
}
