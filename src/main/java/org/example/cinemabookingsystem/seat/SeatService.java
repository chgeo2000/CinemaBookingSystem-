package org.example.cinemabookingsystem.seat;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.seat.dto.CreateSeatRequest;
import org.example.cinemabookingsystem.seat.dto.SeatAvailability;
import org.example.cinemabookingsystem.seat.rowmappers.SeatAvailabilityRowMapper;
import org.example.cinemabookingsystem.showing.ShowingService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private static final String SEAT_AVAILABILITY_SQL = """
            SELECT s.id            AS seat_id,
                   s.row_number    AS row_number,
                   s.column_number AS column_number,
                   s.seat_type     AS seat_type,
                   bs.id IS NOT NULL AS booked
              FROM seat s
              LEFT JOIN booked_seat bs
                ON bs.seat_id = s.id AND bs.showing_id = :showingId
             ORDER BY s.id
            """;

    private final SeatRepository seatRepository;
    private final ShowingService showingService;
    private final JdbcClient jdbcClient;

    public List<Seat> findAll() {
        return seatRepository.findAll();
    }

    public Seat create(CreateSeatRequest request) {
        return seatRepository.save(new Seat(null, request.rowNumber(), request.columnNumber(), request.seatType()));
    }

    public List<SeatAvailability> availabilityForShowing(Long showingId) {
        showingService.findById(showingId);
        return jdbcClient.sql(SEAT_AVAILABILITY_SQL)
                .param("showingId", showingId)
                .query(new SeatAvailabilityRowMapper())
                .list();
    }
}
