package com.cobamovil.backend.security;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400}") // en segundos
    private int jwtExpirationInSeconds;

    private void validateSecret() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 32 caracteres.");
        }
    }

    private SecretKey getSigningKey() {
        validateSecret();
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (jwtExpirationInSeconds * 1000L);
        String jti = java.util.UUID.randomUUID().toString();

        return Jwts.builder()
            .subject(userPrincipal.getUsername())
            .id(jti)
            .issuedAt(new Date(nowMillis))
            .expiration(new Date(expMillis))
            .signWith(getSigningKey())
            .compact();
    }

    public void revokeToken(String token) {
        String jti = getJtiFromJWT(token);
        if (jti != null) {
            tokenBlacklist.add(jti);
        }
    }

    public String getJtiFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getId();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken)
                .getPayload();
            if (claims.getId() != null && tokenBlacklist.contains(claims.getId())) {
                logger.warn("Token JWT revocado (jti en blacklist): {}", claims.getId());
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.error("Error de validación JWT: {}", ex.getMessage());
            return false;
        }
    }

    public int getExpirationInSeconds() {
        return jwtExpirationInSeconds;
    }
}
