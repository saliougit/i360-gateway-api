package com.innov4africa.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${spring.security.jwt.secret}")
    private String secret;

    @Value("${spring.security.jwt.expiration}")
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }    public Boolean validateToken(String token) {
        try {
            // 1. D'abord vérifier la signature et l'expiration
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (isTokenExpired(token)) {
                return false;
            }

            // 2. Vérifier les claims requis pour iPay
            String ipayToken = claims.get("ipayToken", String.class);
            String accountIdIPay = claims.get("accountIdIPay", String.class);
            String userId = claims.get("userId", String.class);

            // Vérifier que les claims essentiels sont présents
            return ipayToken != null && !ipayToken.isEmpty() 
                && accountIdIPay != null && !accountIdIPay.isEmpty()
                && userId != null && !userId.isEmpty();

        } catch (Exception e) {
            return false;
        }
    }
}
