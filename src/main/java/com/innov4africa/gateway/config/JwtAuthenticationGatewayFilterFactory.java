package com.innov4africa.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innov4africa.gateway.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationGatewayFilterFactory(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 1. Vérifier la présence du token
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Tentative d'accès sans token : {}", request.getPath());
                return createErrorResponse(exchange, "Token d'authentification manquant", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 2. Valider le token
            try {
                if (!jwtUtil.validateToken(token)) {
                    log.warn("Token invalide détecté pour : {}", request.getPath());
                    return createErrorResponse(exchange, "Token invalide ou expiré", HttpStatus.FORBIDDEN);
                }

                // 3. Extraire les informations utilisateur
                String username = jwtUtil.extractUsername(token);
                log.info("Accès autorisé pour l'utilisateur {} à {}", username, request.getPath());

                // 4. Enrichir la requête avec les informations validées
                ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Authenticated-User", username)
                    .header("X-Gateway-Validated", "true")
                    .build();

                // 5. Transmettre au service avec les informations enrichies
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("Erreur lors de la validation du token", e);
                return createErrorResponse(exchange, "Erreur de validation du token", HttpStatus.FORBIDDEN);
            }
        };
    }

    private Mono<Void> createErrorResponse(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    public static class Config {
        // Configuration si nécessaire
    }
}