package no.idporten.eudiw.byob.service.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info().title("Bring Your Own Bevis-API").version("1.0").description("API for å " +
                "enkelt kunne legge til nye bevistyper i sandkassemiljøet. Her kan du også se alle bevistypene som er laget " +
                "med BYOB (Bring Your Own Bevis) utstederen, samt søke blant bevisene."));
    }
}