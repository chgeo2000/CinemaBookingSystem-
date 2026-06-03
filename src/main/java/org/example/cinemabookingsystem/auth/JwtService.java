package org.example.cinemabookingsystem.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateToken(String userName) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + jwtProperties.expirationMillis());
        return Jwts.builder()
                .subject(userName)
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey())
                .compact();
    }

    public String extractUserName(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            String userName = extractUserName(token);
            return userName.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return parse(token).getExpiration().before(new Date());
    }

    private io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
