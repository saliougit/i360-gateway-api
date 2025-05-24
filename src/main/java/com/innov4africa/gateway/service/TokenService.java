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
    private static final String USER_TOKEN_KEY_PREFIX = "user:";

    private volatile boolean isRedisDown = false;    
    public TokenService(
            @Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate, 
            JwtUtil jwtUtil,
            Counter redisDownCounter,
            Counter fallbackJwtValidationCounter) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.redisDownCounter = redisDownCounter;
        this.fallbackJwtValidationCounter = fallbackJwtValidationCounter;
    }    public Mono<Boolean> validateToken(String token, String username) {
        // 1. Valider d'abord la signature JWT
        if (!jwtUtil.validateToken(token)) {
            return Mono.just(false);
        }

        // 2. Si Redis est déjà marqué comme down, passer directement en mode dégradé
        if (isRedisDown) {
            logger.warn("Redis est down, validation en mode dégradé pour l'utilisateur: {}", username);
            fallbackJwtValidationCounter.increment();
            return Mono.just(true);
        }

        // 3. Vérifier le token dans Redis
        return redisTemplate.opsForValue()
            .get(USER_TOKEN_KEY_PREFIX + username)
            .map(storedToken -> {
                // Token trouvé dans Redis
                isRedisDown = false; // Reset le flag si Redis répond
                return storedToken.equals(token);
            })
            .onErrorResume(e -> {
                logger.error("Erreur Redis lors de la validation du token: {}", e.getMessage());
                if (!isRedisDown) {
                    // Premier échec Redis
                    isRedisDown = true;
                    redisDownCounter.increment();
                    logger.warn("Passage en mode dégradé - validation JWT uniquement");
                }
                fallbackJwtValidationCounter.increment();
                return Mono.just(true); // Mode dégradé : validation JWT uniquement
            })
            .defaultIfEmpty(true); // Si pas trouvé dans Redis, considérer valide (cas migration)
    }    public Mono<Void> saveToken(String username, String token) {
        // Si Redis est down, ne pas essayer de sauvegarder
        if (isRedisDown) {
            logger.warn("Redis est down, impossible de sauvegarder le token pour: {}", username);
            return Mono.empty();
        }

        // Calculer le TTL basé sur l'expiration du JWT
        Long expiration = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        Duration ttl = Duration.ofMillis(expiration);

        return invalidateToken(username) // D'abord invalider l'ancien token
            .then(redisTemplate.opsForValue()
                .set(USER_TOKEN_KEY_PREFIX + username, token, ttl))
            .doOnSuccess(success -> {
                isRedisDown = false; // Reset le flag si Redis répond
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
    }    public Mono<Boolean> invalidateToken(String username) {
        if (isRedisDown) {
            logger.warn("Redis est down, impossible d'invalider le token pour: {}", username);
            return Mono.just(false);
        }

        return redisTemplate.delete(USER_TOKEN_KEY_PREFIX + username)
            .map(deletedCount -> deletedCount > 0) // Convertit Long en Boolean
            .onErrorResume(e -> {
                logger.error("Erreur Redis lors de l'invalidation du token: {}", e.getMessage());
                if (!isRedisDown) {
                    isRedisDown = true;
                    redisDownCounter.increment();
                }
                return Mono.just(false);
            });
    }
}
