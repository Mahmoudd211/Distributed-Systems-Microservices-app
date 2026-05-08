package com.homesvc.booking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public final class JwtUtil {

    private static final String SECRET = "HomeSvcPlatform2024SecretKey";

    private JwtUtil() {
    }

    private static SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Long extractUserId(String token) {
        Claims c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        Object uid = c.get("userId");
        if (uid instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(c.getSubject());
    }

    public static String extractRole(String token) {
        Claims c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return c.get("role", String.class);
    }
}
