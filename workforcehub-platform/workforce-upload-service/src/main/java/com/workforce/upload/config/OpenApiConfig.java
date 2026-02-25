package com.workforce.upload.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI workforceUploadOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workforce Upload Service API")
                        .description("Production-grade microservice for bulk workforce data uploads")
                        .version("v1")
                        .contact(new Contact().name("WorkforceHub Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local development")
                ));
    }
}
