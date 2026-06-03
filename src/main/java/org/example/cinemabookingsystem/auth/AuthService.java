package org.example.cinemabookingsystem.auth;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.auth.dto.AuthResponse;
import org.example.cinemabookingsystem.auth.dto.LoginRequest;
import org.example.cinemabookingsystem.auth.dto.RegisterRequest;
import org.example.cinemabookingsystem.user.CinemaUser;
import org.example.cinemabookingsystem.user.CinemaUserRepository;
import org.example.cinemabookingsystem.user.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CinemaUserRepository cinemaUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (cinemaUserRepository.existsByUserName(request.userName())) {
            throw new UserAlreadyExistsException("User name already in use: " + request.userName());
        }
        CinemaUser saved = cinemaUserRepository.save(new CinemaUser(
                null,
                request.userName(),
                passwordEncoder.encode(request.password()),
                Role.USER,
                request.email(),
                request.phoneNumber()
        ));
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.userName(), request.password())
        );
        CinemaUser user = cinemaUserRepository.findByUserName(request.userName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.userName()));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(CinemaUser user) {
        return new AuthResponse(
                jwtService.generateToken(user.userName()),
                user.userName(),
                user.role().name()
        );
    }
}
