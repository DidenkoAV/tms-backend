package com.test.system.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Test Case Management System API")
                        .version("v1.0")
                        .description("""
                                ## Authentication

                                This API uses JWT Bearer tokens for authentication.

                                ### How to authenticate:

                                1. **Register a new user** (if needed):
                                   - Use `POST /api/auth/register` endpoint
                                   - Provide email, password, and full name

                                2. **Login to get JWT token**:
                                   - Use `POST /api/auth/login` endpoint
                                   - Provide your email and password
                                   - Copy the `token` from the response

                                3. **Authorize in Swagger**:
                                   - Click the 🔓 **Authorize** button at the top
                                   - Paste your JWT token (without "Bearer " prefix)
                                   - Click **Authorize** and then **Close**

                                4. **Make authenticated requests**:
                                   - All protected endpoints will now include your token automatically

                                ### Alternative: Personal Access Tokens (PAT)

                                You can also use Personal Access Tokens for API integrations:
                                - Create a PAT via `POST /api/tokens` (requires JWT auth first)
                                - Use the full PAT token (including `pat_` prefix) in the Authorization header
                                """))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (from /api/auth/login response) or Personal Access Token")
                        ))
                // apply Bearer globally to all operations
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
