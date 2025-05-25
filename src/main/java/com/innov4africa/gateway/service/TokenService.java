package com.innov4africa.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.innov4africa.gateway.security.JwtUtil;
import io.micrometer.core.instrument.Counter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class TokenService {
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final Counter redisDownCounter;
    private final Counter fallbackJwtValidationCounter;
    private static final String USER_TOKEN_KEY_PREFIX = "user_tokens:";

    private volatile boolean isRedisDown = false;    

    public TokenService(
            @Qualifier("tokenRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate, 
            JwtUtil jwtUtil,
            Counter redisDownCounter,
            Counter fallbackJwtValidationCounter) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.redisDownCounter = redisDownCounter;
        this.fallbackJwtValidationCounter = fallbackJwtValidationCounter;
    }

    public Mono<Boolean> validateToken(String token, String username) {
        // 1. Valider d'abord la signature JWT
        if (!jwtUtil.validateToken(token)) {
            logger.warn("Token JWT invalide pour l'utilisateur: {}", username);
            return Mono.just(false);
        }

        // Extraire le username du token pour double vérification
        String tokenUsername = jwtUtil.extractUsername(token);
        if (!username.equals(tokenUsername)) {
            logger.warn("Username mismatch. Token: {}, Request: {}", tokenUsername, username);
            return Mono.just(false);
        }        // 2. Si Redis est down, on accepte le token si le JWT est valide
        if (isRedisDown) {
            logger.warn("Redis est down, acceptation du token JWT valide pour l'utilisateur: {}", username);
            fallbackJwtValidationCounter.increment();
            return Mono.just(true);
        }

        // 3. Si Redis est up, vérifier le token dans Redis
        String key = USER_TOKEN_KEY_PREFIX + username;
        return redisTemplate.opsForValue()
            .get(key)
            .map(storedToken -> {
                isRedisDown = false; // Reset le flag si Redis répond
                boolean isValid = token.equals(storedToken);
                if (!isValid) {
                    logger.warn("Token non valide pour l'utilisateur: {}. Token ne correspond pas à celui stocké.", username);
                }
                return isValid;
            })
            .onErrorResume(e -> {                logger.error("Erreur Redis lors de la validation du token: {}", e.getMessage());
                if (!isRedisDown) {
                    isRedisDown = true;
                    redisDownCounter.increment();
                    logger.warn("Redis down - passage en mode fallback JWT");
                }
                // En cas d'erreur Redis, on accepte le token puisque le JWT est déjà validé
                return Mono.just(true);
            })
            .defaultIfEmpty(false);
    }

    public Mono<Void> saveToken(String username, String token) {
        if (username == null || username.trim().isEmpty()) {
            logger.error("Tentative de sauvegarde de token avec un username null ou vide");
            return Mono.empty();
        }

        // Si Redis est down, ne pas essayer de sauvegarder
        if (isRedisDown) {
            logger.warn("Redis est down, impossible de sauvegarder le token pour: {}", username);
            return Mono.empty();
        }

        // Calculer le TTL basé sur l'expiration du JWT
        Long expiration = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        Duration ttl = Duration.ofMillis(expiration);

        String key = USER_TOKEN_KEY_PREFIX + username;
        return redisTemplate.opsForValue()
            .set(key, token, ttl)
            .doOnSuccess(success -> {
                isRedisDown = false;
                logger.debug("Token sauvegardé avec succès pour: {}", username);
            })
            .onErrorResume(e -> {
                if (!isRedisDown) {
                    isRedisDown = true;
                    redisDownCounter.increment();
                    logger.error("Redis down, passage en mode dégradé. Erreur: {}", e.getMessage());
                }
                return Mono.empty();
            })
            .then();
    }

    public Mono<Boolean> invalidateToken(String username) {
        if (isRedisDown) {
            logger.warn("Redis est down, impossible d'invalider les tokens pour: {}", username);
            return Mono.just(false);
        }

        String key = USER_TOKEN_KEY_PREFIX + username;
        return redisTemplate.delete(key)
            .map(deletedCount -> deletedCount > 0)
            .onErrorResume(e -> {
                logger.error("Erreur Redis lors de l'invalidation des tokens: {}", e.getMessage());
                if (!isRedisDown) {
                    isRedisDown = true;
                    redisDownCounter.increment();
                }
                return Mono.just(false);
            });
    }
}
