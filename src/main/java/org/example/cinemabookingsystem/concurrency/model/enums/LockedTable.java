package org.example.cinemabookingsystem.concurrency.model.enums;

public enum LockedTable {
    MOVIE("movie"),
    SHOWING("showing"),
    SEAT("seat"),
    BOOKING("booking"),
    BOOKED_SEAT("booked_seat"),
    CINEMA_USER("cinema_user");

    private final String sqlTableName;

    LockedTable(String sqlTableName) {
        this.sqlTableName = sqlTableName;
    }

    public String sqlTableName() {
        return sqlTableName;
    }
}
