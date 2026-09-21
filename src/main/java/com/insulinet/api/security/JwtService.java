package com.insulinet.api.security;

import com.insulinet.api.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpireMinutes;

    public JwtService(AppProperties appProperties) {
        this.key = Keys.hmacShaKeyFor(
                appProperties.jwt().secretKey().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireMinutes = appProperties.jwt().accessTokenExpireMinutes();
    }

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenExpireMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /**
     * @throws JwtException se o token for invalido, malformado ou expirado.
     */
    public Long extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("Subject invalido no token.");
        }
    }
}
