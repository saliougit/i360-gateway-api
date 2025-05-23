package com.innov4africa.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import com.innov4africa.gateway.security.JwtUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    private final List<String> publicPaths = List.of(
        "/auth/login",
        "/auth/register",
        "/auth/logout",
        "/",
        "/swagger-ui.html",
        "/swagger-ui/",
        "/v3/api-docs/",
        "/webjars/",
        "/swagger-resources/",
        "/favicon.ico"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        logger.debug("JWT Filter processing request for path: {}", path);
        
        // Skip verification for public endpoints
        if (publicPaths.stream().anyMatch(path::startsWith)) {
            logger.debug("Public path detected, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorizedResponse(exchange, "Token d'authentification manquant ou invalide");
        }

        String token = authHeader.substring(7);

        try {
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                logger.debug("Valid JWT token for user: {} accessing path: {}", username, path);
                
                // Add authentication to the context
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

                // Add useful headers that might be needed by downstream services
                ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-Auth-User", username)
                    .build();

                exchange = exchange.mutate().request(request).build();

                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            }
        } catch (Exception e) {
            logger.error("Error validating JWT token", e);
        }

        return sendUnauthorizedResponse(exchange, "Token d'authentification expiré ou invalide");
    }

    private Mono<Void> sendUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String jsonBody = String.format("{\"status\":\"error\",\"message\":\"%s\"}", message);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)))
                      .then(response.setComplete());
    }
}
