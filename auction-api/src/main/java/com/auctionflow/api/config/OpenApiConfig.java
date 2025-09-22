package com.auctionflow.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Auction Flow API",
        version = "v1",
        description = "REST API for Auction Flow - Production-grade auction backend with real-time bidding",
        contact = @io.swagger.v3.oas.annotations.info.Contact(
            name = "Auction Flow Support",
            email = "support@auctionflow.com"
        ),
        license = @io.swagger.v3.oas.annotations.info.License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "https://api.auctionflow.com", description = "Production Server"),
        @Server(url = "http://localhost:8080", description = "Local Development Server")
    },
    externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
        description = "Developer Portal",
        url = "/developer-portal"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}