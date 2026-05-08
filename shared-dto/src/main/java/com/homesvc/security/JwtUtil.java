package com.homesvc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Template: copy into each Spring Boot service and adjust package if needed.
 */
public class JwtUtil {

    private final String secret;
    private final long expirationMs;

    public JwtUtil(String secret, long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        Claims c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        Object uid = c.get("userId");
        if (uid instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(c.getSubject());
    }

    public String extractRole(String token) {
        Claims c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return c.get("role", String.class);
    }
}
