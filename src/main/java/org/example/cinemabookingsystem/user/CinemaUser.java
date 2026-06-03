package org.example.cinemabookingsystem.user;

public record CinemaUser(
        Long id,
        String userName,
        String password,
        Role role,
        String email,
        String phoneNumber
) {
}
