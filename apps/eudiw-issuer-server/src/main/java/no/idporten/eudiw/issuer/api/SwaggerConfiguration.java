package no.idporten.eudiw.issuer.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    public final static String API_TAG = "eudiw-issuer-api-v1";
    public static final String API_DESCRIPTION = "eIDAS 2.0 NO Sandbox Credential Issuer API";

    @Bean
    public OpenAPI openAPIConfig() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("Maskinporten", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Maskinporten access token.  Must have credential issuer as single audience, be end-user restricted, and contain scope for credential configuration.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

}
