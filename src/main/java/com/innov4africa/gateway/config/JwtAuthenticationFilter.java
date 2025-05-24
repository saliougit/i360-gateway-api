package com.innov4africa.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import com.innov4africa.gateway.security.JwtUtil;
import com.innov4africa.gateway.service.TokenService;

import java.util.Collections;

public class JwtAuthenticationFilter implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenService tokenService) {
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
    }

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        logger.debug("Processing request for path: {}", path);

        // Vérifier s'il y a un header Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                
                return tokenService.validateToken(token, username)
                    .flatMap(isValid -> {
                        if (isValid) {
                            logger.debug("Token validé pour l'utilisateur : {}", username);
                            
                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                            // Ajouter l'authentification au contexte de sécurité
                            return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                        } else {
                            logger.warn("Token invalide ou expiré pour l'utilisateur : {}", username);
                            return onError(exchange.getResponse(), "Token invalide ou expiré", HttpStatus.UNAUTHORIZED);
                        }
                    })
                    .onErrorResume(e -> {
                        logger.error("Erreur lors de la validation du token", e);
                        return onError(exchange.getResponse(), "Erreur de validation du token", HttpStatus.UNAUTHORIZED);
                    });
            } catch (Exception e) {
                logger.error("Erreur lors de l'extraction du username", e);
                return onError(exchange.getResponse(), "Token mal formé", HttpStatus.UNAUTHORIZED);
            }
        }

        // Si aucun token n'est trouvé, continuer la chaîne
        return chain.filter(exchange);
    }    private Mono<Void> onError(ServerHttpResponse response, String message, HttpStatus status) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(status);
        
        String errorJson = String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", 
            status.value(), 
            status.getReasonPhrase(),
            message);
            
        byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        
        return response.writeWith(Mono.just(buffer));
    }
}