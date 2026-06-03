package org.example.cinemabookingsystem.movie;

public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long id) {
        super("Movie not found: " + id);
    }
}
