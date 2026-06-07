package org.example.cinemabookingsystem.concurrency.model.enums;

public enum LockedTable {
    MOVIE,
    SHOWING,
    SEAT,
    BOOKING,
    BOOKED_SEAT,
    CINEMA_USER;

    /**
     * The physical Postgres table name. Relies on the convention that each constant's
     * {@code UPPER_SNAKE_CASE} name is the {@code lower_snake_case} table name
     * (e.g. {@code BOOKED_SEAT} → {@code booked_seat}).
     */
    public String sqlTableName() {
        return name().toLowerCase();
    }
}
