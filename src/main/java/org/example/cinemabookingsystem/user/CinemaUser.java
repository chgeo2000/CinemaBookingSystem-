package org.example.cinemabookingsystem.user;

/**
 * A registered user of the cinema booking system.
 *
 * <p>Users authenticate via JWT and interact with the system according to their {@link Role}:
 * a {@code USER} can browse content and manage their own bookings; an {@code ADMIN} can
 * additionally create and delete movies, showings, and seats, and trigger concurrency demo scenarios.
 *
 * <p>{@code password} is always stored as a BCrypt hash — never in plain text.
 * {@code userName} is unique across the system (enforced at the schema level).
 */
public record CinemaUser(
        Long id,
        String userName,
        String password,
        Role role,
        String email,
        String phoneNumber
) {
}
