package org.example.cinemabookingsystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 50) String userName,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Email @Size(max = 50) String email,
        @Size(max = 10) String phoneNumber
) {
}
