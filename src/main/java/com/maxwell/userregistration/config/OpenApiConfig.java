package com.maxwell.userregistration.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Auth API")
                        .description("""
                                Secure user registration and authentication backend.

                                **Auth flow:**
                                1. `POST /api/auth/register` → check email, verify captcha
                                2. `GET /api/auth/verify-email?token=` → activate account
                                3. `POST /api/auth/login` → get tokens (or MFA challenge)
                                4. `POST /api/auth/mfa/validate` → complete MFA, get tokens
                                5. `POST /api/auth/refresh` → rotate refresh token

                                Paste your access token in the **Authorize** button above to call protected endpoints.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Maxwell Aboagye")
                                .url("https://github.com/maxxy21/spring-auth-api")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your access token here (without 'Bearer ' prefix)")
                        )
                );
    }
}
