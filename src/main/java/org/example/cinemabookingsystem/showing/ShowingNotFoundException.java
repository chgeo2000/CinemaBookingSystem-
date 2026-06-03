package org.example.cinemabookingsystem.showing;

public class ShowingNotFoundException extends RuntimeException {

    public ShowingNotFoundException(Long id) {
        super("Showing not found: " + id);
    }
}
