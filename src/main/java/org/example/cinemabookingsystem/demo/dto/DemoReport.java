package org.example.cinemabookingsystem.demo.dto;

import java.util.List;

public record DemoReport(
        String scenario,
        int commits,
        int aborts,
        List<DemoTransactionResult> results
) {
    public static DemoReport summarise(String scenario, List<DemoTransactionResult> results) {
        int commits = (int) results.stream().filter(r -> "COMMITTED".equals(r.outcome())).count();
        return new DemoReport(scenario, commits, results.size() - commits, results);
    }
}
