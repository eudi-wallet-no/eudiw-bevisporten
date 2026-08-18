package no.idporten.eudiw.statuslist.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Status-list API")
                        .version("1.0.0")
                        .description("Status provider API for status-lister og Status issuer internal API for allocate and revoke status on status-list"));
    }
}
