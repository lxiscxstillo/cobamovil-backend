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
    // Blacklist de JWT (jti)
    private static final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();


    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private void validateSecret() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 32 caracteres y estar definido por variable de entorno segura.");
        }
    }

    @Value("${app.jwt.expiration:86400}")
    private int jwtExpirationInMs;

    private SecretKey getSigningKey() {
        validateSecret();
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + jwtExpirationInMs * 1000L;
        String jti = java.util.UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
            .subject(userPrincipal.getUsername())
            .issuedAt(new Date(nowMillis))
            .expiration(new Date(expMillis))
            .claim("jti", jti)
            .signWith(getSigningKey());

        return builder.compact();
    }

    // Método para revocar un token (agregar jti a blacklist)
    public void revokeToken(String token) {
        String jti = getJtiFromJWT(token);
        if (jti != null) {
            tokenBlacklist.add(jti);
        }
    }

    // Extraer jti del token
    public String getJtiFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object jtiObj = claims.get("jti");
        return jtiObj != null ? jtiObj.toString() : null;
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
            String jti = claims.get("jti", String.class);
            if (jti != null && tokenBlacklist.contains(jti)) {
                logger.warn("Token JWT revocado (jti en blacklist): {}", jti);
                return false;
            }
            return true;
        } catch (SecurityException ex) {
            logger.error("Token JWT inválido: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Token JWT malformado: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Token JWT expirado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Token JWT no soportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("Claims JWT vacíos: {}", ex.getMessage());
        }
        return false;
    }
}