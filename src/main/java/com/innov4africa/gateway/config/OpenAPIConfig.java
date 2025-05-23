package com.innov4africa.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lightweight API Gateway")
                        .version("1.0")
                        .description("Gateway léger pour la gestion des routes et de la sécurité")
                        .contact(new Contact()
                                .name("Innov4Africa")
                                .email("contact@innov4africa.sn")
                                .url("https://innov4africa.sn"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://innov4africa.sn/terms")))
                .addServersItem(new Server().url("http://localhost:8080").description("Local Gateway"))
                .addServersItem(new Server().url("https://api.innov4africa.sn").description("Production Gateway"));
    }
}
