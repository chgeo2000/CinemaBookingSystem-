package org.example.cinemabookingsystem.booking;

/**
 * Encodes a {@code (showingId, seatId)} pair into a single {@code long} resource identifier
 * usable as a {@code LockedTable.BOOKED_SEAT} lock key.
 *
 * <p>The lock granularity matches the row that the booking flow is about to insert into
 * {@code booked_seat}, so two bookings of <i>the same seat for the same showing</i> conflict,
 * while bookings of the same seat for <i>different showings</i> do not.
 *
 * <p>Encoding: {@code showingId * SEAT_MULTIPLIER + seatId}. With a multiplier of one million
 * we support seat IDs up to {@code 999_999} per showing and showing IDs up to
 * {@code Long.MAX_VALUE / 1_000_000} (~9.2 trillion). Both bounds are checked at encode time.
 */
public final class SeatLockKey {

    static final long SEAT_MULTIPLIER = 1_000_000L;

    private SeatLockKey() {
        throw new IllegalStateException("Utility class");
    }

    public static long encode(long showingId, long seatId) {
        if (seatId < 0 || seatId >= SEAT_MULTIPLIER) {
            throw new IllegalArgumentException("seatId must be in [0, " + SEAT_MULTIPLIER + "): " + seatId);
        }
        if (showingId < 0 || showingId > Long.MAX_VALUE / SEAT_MULTIPLIER) {
            throw new IllegalArgumentException("showingId out of supported range: " + showingId);
        }
        return showingId * SEAT_MULTIPLIER + seatId;
    }
}
