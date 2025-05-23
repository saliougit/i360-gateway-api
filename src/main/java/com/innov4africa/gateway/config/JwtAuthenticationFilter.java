// package com.innov4africa.gateway.config;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.server.reactive.ServerHttpRequest;
// import org.springframework.http.server.reactive.ServerHttpResponse;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.ReactiveSecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.web.server.ServerWebExchange;
// import org.springframework.web.server.WebFilter;
// import org.springframework.web.server.WebFilterChain;
// import reactor.core.publisher.Mono;
// import com.innov4africa.gateway.security.JwtUtil;

// import java.nio.charset.StandardCharsets;
// import java.util.Collections;
// import java.util.List;

// @Component
// public class JwtAuthenticationFilter implements WebFilter {
//     private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

//     @Autowired
//     private JwtUtil jwtUtil;

//     private final List<String> publicPaths = List.of(
//         "/auth/login",
//         "/auth/register",
//         "/auth/logout",
//         "/swagger-ui.html",
//         "/swagger-ui/",
//         "/v3/api-docs/",
//         "/webjars/",
//         "/swagger-resources/",
//         "/favicon.ico"
//     );

//     private boolean isPublicPath(String path) {
//         logger.debug("--- START JWT Filter for request path: {} ---", path);


//         if (path.equals("/")) {
//             logger.debug("Path '/' is public.");
//             return true;
//         }

//         for (String publicPath : publicPaths) {
//             if (publicPath.endsWith("/")) {
//                 if (path.startsWith(publicPath)) {
//                     logger.debug("Path '{}' starts with public prefix '{}'. It is public.", path, publicPath);
//                     return true;
//                 }
//             } else {
//                 if (path.equals(publicPath)) {
//                     logger.debug("Path '{}' exactly equals public path '{}'. It is public.", path, publicPath);
//                     return true;
//                 }
//             }
//         }
//         logger.debug("Path '{}' is NOT public.", path);
//         return false;
//     }

//     @Override
//     public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//         String path = exchange.getRequest().getPath().value();
        
//         logger.debug("--- START JWT Filter for request path: {} ---", path);
        
//         // Skip verification for public endpoints
//         if (isPublicPath(path)) {
//             logger.debug("Public path detected, skipping JWT validation: {}", path);
//             return chain.filter(exchange);
//         }

//         String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             return sendUnauthorizedResponse(exchange, "Token d'authentification manquant ou invalide");
//         }

//         String token = authHeader.substring(7);

//         try {
//             if (jwtUtil.validateToken(token)) {
//                 String username = jwtUtil.extractUsername(token);
//                 logger.debug("Valid JWT token for user: {} accessing path: {}", username, path);
                
//                 // Add authentication to the context
//                 UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
//                     username,
//                     null,
//                     Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
//                 );

//                 // Add useful headers that might be needed by downstream services
//                 ServerHttpRequest request = exchange.getRequest().mutate()
//                     .header("X-Auth-User", username)
//                     .build();

//                 exchange = exchange.mutate().request(request).build();

//                 return chain.filter(exchange)
//                     .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
//             }
//         } catch (Exception e) {
//             logger.error("Error validating JWT token", e);
//         }

//         return sendUnauthorizedResponse(exchange, "Token d'authentification expiré ou invalide");
//     }

//     private Mono<Void> sendUnauthorizedResponse(ServerWebExchange exchange, String message) {
//         ServerHttpResponse response = exchange.getResponse();
//         response.setStatusCode(HttpStatus.UNAUTHORIZED);
//         response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
//         String jsonBody = String.format("{\"status\":\"error\",\"message\":\"%s\"}", message);
//         byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        
//         return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)))
//                       .then(response.setComplete());
//     }
// }


package com.innov4africa.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // N'oubliez pas cette annotation
import org.springframework.cloud.gateway.filter.GatewayFilter; // <--- CHANGEMENT ICI
import org.springframework.cloud.gateway.filter.GatewayFilterChain; // <--- CHANGEMENT ICI
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
import reactor.core.publisher.Mono;
import com.innov4africa.gateway.security.JwtUtil; // Assurez-vous que JwtUtil est bien injecté

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GatewayFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Les chemins publics sont gérés ici car ce filtre est appliqué par route
    // et nous voulons qu'il s'exécute pour TOUTES les routes qui l'appliquent
    // La logique isPublicPath était plus utile pour un WebFilter global.
    // Maintenant, nous allons gérer l'exclusion dans la configuration YAML.

    // Retirez ou commentez la logique isPublicPath et publicPaths si vous l'ajoutez route par route
    // Ou adaptez-la pour qu'elle exclue les routes dans le filtre lui-même si vous préférez
    // la rendre globale et l'exclure pour /auth/login dans le filtre (comme mon exemple initial)

    // Pour l'instant, simplifions en nous basant sur l'application via YAML:
    // C'est votre choix de rendre le filtre plus intelligent ou de le configurer par route.
    // Si vous le voulez appliqué uniquement aux routes PROTEGEES via YAML, vous n'avez plus besoin de publicPaths ici.

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) { // <--- CHANGEMENT ICI (GatewayFilterChain)
        String path = exchange.getRequest().getPath().value();

        logger.debug("--- START JWT GatewayFilter for request path: {} ---", path); 

        // Pas de logique isPublicPath ici si vous l'appliquez seulement aux routes protégées via YAML
        // Si vous l'appliquez globalement et gérez l'exclusion ici, remettez la logique isPublicPath.
        // Pour l'exemple, supposons qu'il est appliqué à des routes protégées via YAML.

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("Missing or invalid Authorization header for path: {}", path); // AJOUT DE LOG
            return sendUnauthorizedResponse(exchange, "Token d'authentification manquant ou invalide");
        }

        String token = authHeader.substring(7);

        try {
            if (jwtUtil.validateToken(token)) { // Assurez-vous que jwtUtil.validateToken() vérifie la signature ET l'expiration
                String username = jwtUtil.extractUsername(token);
                logger.debug("Valid JWT token for user: {} accessing path: {}", username, path);

                // Add authentication to the context for Spring Security
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username,
                    null, // credentials can be null if not needed downstream
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // Rôles depuis le JWT ou par défaut
                );

                // Ajoutez l'authentification au ReactiveSecurityContextHolder
                // Ceci est CRUCIAL pour que Spring Security reconnaisse l'utilisateur
                return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

                // Optionnel: Ajouter des headers pour les services en aval (si nécessaire)
                // ServerHttpRequest request = exchange.getRequest().mutate()
                //     .header("X-Auth-User", username)
                //     .build();
                // return chain.filter(exchange.mutate().request(request).build())
                //             .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

            } else { // Si validateToken retourne false mais ne lance pas d'exception
                logger.warn("JWT token validation failed for path: {}. jwtUtil.validateToken returned false.", path); // AJOUT DE LOG
                return sendUnauthorizedResponse(exchange, "Token d'authentification invalide");
            }
        } catch (Exception e) {
            logger.error("Error validating JWT token for path: {}. Error: {}", path, e.getMessage(), e); // AJOUT DE LOG
            return sendUnauthorizedResponse(exchange, "Token d'authentification expiré ou invalide");
        }
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