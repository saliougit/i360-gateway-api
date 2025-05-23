// package com.innov4africa.gateway.config;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
// import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
// import org.springframework.security.config.web.server.ServerHttpSecurity;
// import org.springframework.security.web.server.SecurityWebFilterChain;
// import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.reactive.CorsConfigurationSource;
// import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

// import java.util.List;

// @Configuration
// @EnableWebFluxSecurity
// public class SecurityConfig {

//     @Autowired
//     private JwtAuthenticationFilter jwtAuthenticationFilter;

//     @Bean
//     public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//         return http
//                 .csrf(csrf -> csrf.disable())
//                 .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                 // Use NoOpServerSecurityContextRepository to avoid storing security context
//                 // and rely only on JWT tokens for each request
//                 .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
//                 .authorizeExchange(exchanges -> exchanges
//                         // Public paths that don't require authentication
//                         .pathMatchers("/auth/login", "/auth/register", "/auth/logout").permitAll()
//                         .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
//                                 "/webjars/**", "/swagger-resources/**", "/favicon.ico").permitAll()
//                         // All other paths require authentication
//                         .anyExchange().authenticated()
//                 )
//                 .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
//                 .httpBasic(basic -> basic.disable())
//                 .formLogin(form -> form.disable())
//                 .build();
//     }

//     @Bean
//     public CorsConfigurationSource corsConfigurationSource() {
//         CorsConfiguration configuration = new CorsConfiguration();
//         configuration.setAllowedOrigins(List.of("*"));
//         configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
//         configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Origin", "Accept"));
//         configuration.setExposedHeaders(List.of("Authorization"));
//         configuration.setMaxAge(3600L);

//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", configuration);
//         return source;
//     }
// }

package com.innov4africa.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // N'injectez plus JwtAuthenticationFilter directement ici
    // @Autowired
    // private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Use NoOpServerSecurityContextRepository to avoid storing security context
                // and rely only on JWT tokens for each request
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        // Public paths that don't require authentication,
                        // these will be handled by the Gateway's routing without the JWT filter
                        .pathMatchers(
                            "/auth/login", "/auth/register", "/auth/logout",
                            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                            "/webjars/**", "/swagger-resources/**", "/favicon.ico",
                            "/" // La racine est souvent publique
                        ).permitAll()
                        // All other paths will be handled by the Gateway's routes.
                        // For authenticated routes, you apply the JwtAuthenticationFilter
                        // directly in application.yml for those specific routes.
                        // Here, we just state that all *other* requests must be authenticated
                        // assuming the GatewayFilter will handle the authentication.
                        .anyExchange().authenticated()
                )
                // Retirez l'ajout direct du filtre ici, car il est maintenant un GatewayFilter
                // et sera appliqué via application.yml sur des routes spécifiques.
                // .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Origin", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}