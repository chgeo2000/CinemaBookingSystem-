package org.example.cinemabookingsystem.demo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.demo.dto.BulkBookingDemoRequest;
import org.example.cinemabookingsystem.demo.dto.DemoReport;
import org.example.cinemabookingsystem.demo.dto.SingleSeatDemoRequest;
import org.example.cinemabookingsystem.demo.dto.ThreeWayDeadlockDemoRequest;
import org.example.cinemabookingsystem.demo.dto.TwoSeatDeadlockDemoRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ConcurrencyDemoController {

    private final ConcurrencyDemoService demoService;

    @PostMapping("/single-seat-conflict")
    public DemoReport singleSeatConflict(@Valid @RequestBody SingleSeatDemoRequest req) {
        return demoService.singleSeatConflict(req);
    }

    @PostMapping("/classic-deadlock")
    public DemoReport classicDeadlock(@Valid @RequestBody TwoSeatDeadlockDemoRequest req) {
        return demoService.classicDeadlock(req);
    }

    @PostMapping("/three-way-deadlock")
    public DemoReport threeWayDeadlock(@Valid @RequestBody ThreeWayDeadlockDemoRequest req) {
        return demoService.threeWayDeadlock(req);
    }

    @PostMapping("/bulk-booking-atomicity")
    public DemoReport bulkBookingAtomicity(@Valid @RequestBody BulkBookingDemoRequest req) {
        return demoService.bulkBookingAtomicity(req);
    }
}
