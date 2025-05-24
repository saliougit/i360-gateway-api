package com.innov4africa.gateway.controller;

import org.springframework.web.bind.annotation.*;
import com.innov4africa.gateway.service.TokenService;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/internal")
public class TokenController {
    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);
    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }    @PostMapping("/token-refresh")
    public Mono<Void> refreshToken(@RequestBody TokenRefreshRequest request) {
        if (request == null || request.getUsername() == null || request.getToken() == null) {
            logger.error("Invalid token refresh request. Request: {}", request);
            return Mono.error(new IllegalArgumentException("Invalid request"));
        }
        
        logger.info("Received token refresh request for user: {}", request.getUsername());
        logger.debug("Token: {}", request.getToken());
        
        return tokenService.saveToken(request.getUsername(), request.getToken())
            .doOnSuccess(v -> logger.info("Successfully saved token for user: {}", request.getUsername()))
            .doOnError(e -> logger.error("Error saving token for user: {}. Error: {}", request.getUsername(), e.getMessage()));
    }
}
