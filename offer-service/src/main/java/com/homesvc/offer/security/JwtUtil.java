package com.homesvc.offer.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final String secret;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
