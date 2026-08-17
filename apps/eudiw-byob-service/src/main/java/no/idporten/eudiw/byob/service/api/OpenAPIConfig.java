package no.idporten.eudiw.byob.service.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Bring Your Own Bevis-API")
                                .version("1.0")
                                .description("""
                                        API for å administrere bevistyper for test i sandkassemiljøet.  Bevistyper kan opprettes,
                                        endres, hentes ut og søkes på.
                                        
                                        public-endepunktene er for Bevisgeneratoren og brukes for å administrere bevistyper i navnerom "net.eidas2sandkasse*".  
                                        
                                        admin-endepunktene er for andre applikasjoner og har ingen begrensninger på navnerom.
                                        """));
    }
}