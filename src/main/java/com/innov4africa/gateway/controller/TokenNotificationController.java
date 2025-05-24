// package com.innov4africa.gateway.controller;

// import com.innov4africa.gateway.service.TokenService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import reactor.core.publisher.Mono;

// @RestController
// @RequestMapping("/internal")
// public class TokenNotificationController {
//     private static final Logger logger = LoggerFactory.getLogger(TokenNotificationController.class);
//     private final TokenService tokenService;

//     @Autowired
//     public TokenNotificationController(TokenService tokenService) {
//         this.tokenService = tokenService;
//     }    @PostMapping("/token-refresh")
//     public Mono<ResponseEntity<Void>> handleTokenRefresh(@RequestBody TokenRefreshRequest request) {
//         logger.info("Received token refresh notification for username: {}", request.getUsername());

//         // Save the new token and invalidate any old tokens
//         return tokenService.saveToken(request.getUsername(), request.getToken())
//                 .then(Mono.just(ResponseEntity.ok().<Void>build()))
//                 .doOnError(error -> logger.error("Error handling token refresh: {}", error.getMessage()))
//                 .onErrorReturn(ResponseEntity.internalServerError().build());    }
// }
