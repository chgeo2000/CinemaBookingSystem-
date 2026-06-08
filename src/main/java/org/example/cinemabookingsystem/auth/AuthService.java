package org.example.cinemabookingsystem.auth;

import lombok.RequiredArgsConstructor;
import org.example.cinemabookingsystem.auth.dto.AuthResponse;
import org.example.cinemabookingsystem.auth.dto.LoginRequest;
import org.example.cinemabookingsystem.auth.dto.RegisterRequest;
import org.example.cinemabookingsystem.user.CinemaUser;
import org.example.cinemabookingsystem.user.CinemaUserPrincipal;
import org.example.cinemabookingsystem.user.CinemaUserRepository;
import org.example.cinemabookingsystem.user.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
        try {
            Long userId = cinemaUserRepository.save(new CinemaUser(
                    null,
                    request.userName(),
                    passwordEncoder.encode(request.password()),
                    Role.USER,
                    request.email(),
                    request.phoneNumber()
            ));
            return buildAuthResponse(request.userName(), Role.USER, userId);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("User name already in use: " + request.userName());
        }
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.userName(), request.password())
        );
        CinemaUserPrincipal principal = (CinemaUserPrincipal) authentication.getPrincipal();
        return buildAuthResponse(principal.userName(), principal.role(), principal.id());
    }

    private AuthResponse buildAuthResponse(String userName, Role role, Long userId) {
        return new AuthResponse(
                jwtService.generateToken(userName, userId),
                userName,
                role.name()
        );
    }
}
