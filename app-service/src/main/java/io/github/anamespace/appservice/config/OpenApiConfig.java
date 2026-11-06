package io.github.anamespace.appservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenApi(
            @Value("${GATEWAY_PUBLIC_URL:http://localhost:8080}") String gatewayPublicUrl) {
        return new OpenAPI()
                .servers(List.of(new Server()
                        .url(gatewayPublicUrl)
                        .description("Gateway")));
    }
}
