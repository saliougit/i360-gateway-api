// package com.innov4africa.gateway.config;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.cloud.gateway.filter.GatewayFilter;
// import org.springframework.cloud.gateway.filter.GatewayFilterChain;
// import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.ReactiveSecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.web.server.ServerWebExchange;
// import reactor.core.publisher.Mono;
// import com.innov4africa.gateway.security.JwtUtil;

// import java.nio.charset.StandardCharsets;
// import java.util.Collections;

// @Component
// public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

//     private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);

//     @Autowired
//     private JwtUtil jwtUtil;

//     public JwtAuthenticationGatewayFilterFactory() {
//         super(Config.class);
//     }

//     public static class Config {
//         // Aucune configuration spécifique nécessaire pour l'instant
//     }

//     @Override
//     public GatewayFilter apply(Config config) {
//         return (exchange, chain) -> {
//             String path = exchange.getRequest().getPath().value();
            
//             logger.debug("--- START JWT GatewayFilter for request path: {} ---", path); 

//             String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//             logger.debug("Authorization Header for {}: {}", path, authHeader != null ? authHeader : "null");

//             if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                 logger.debug("Missing or invalid Authorization header for path: {}", path);
//                 return sendUnauthorizedResponse(exchange, "Token d'authentification manquant ou invalide");
//             }

//             String token = authHeader.substring(7);
//             logger.debug("Extracted token for {}: {}", path, token);

//             try {
//                 if (jwtUtil.validateToken(token)) {
//                     String username = jwtUtil.extractUsername(token);
//                     logger.debug("Valid JWT token for user: {} accessing path: {}", username, path);
                    
//                     UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
//                         username,
//                         null,
//                         Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
//                     );

//                     return chain.filter(exchange)
//                                 .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

//                 } else {
//                     logger.warn("JWTUtil.validateToken returned FALSE for token: {}", token);
//                     return sendUnauthorizedResponse(exchange, "Token d'authentification invalide");
//                 }
//             } catch (Exception e) {
//                 logger.error("Exception during JWT validation for path: {}. Error: {}", path, e.getMessage(), e);
//                 return sendUnauthorizedResponse(exchange, "Token d'authentification expiré ou invalide");
//             }
//         };
//     }

//     private Mono<Void> sendUnauthorizedResponse(ServerWebExchange exchange, String message) {
//         ServerHttpResponse response = exchange.getResponse();
//         response.setStatusCode(HttpStatus.UNAUTHORIZED);
//         response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
//         String jsonBody = String.format("{\"status\":\"error\",\"message\":\"%s\"}", message);
//         byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        
//         return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)))
//                         .then(response.setComplete());
//     }
// }