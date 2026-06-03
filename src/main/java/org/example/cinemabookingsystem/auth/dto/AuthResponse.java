package org.example.cinemabookingsystem.auth.dto;

public record AuthResponse(
        String token,
        String userName,
        String role
) {
}
