package no.idporten.eudiw.issuer.credentials.status;

import lombok.Data;
import no.idporten.eudiw.issuer.config.APIConnectionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@Configuration
@ConfigurationProperties(prefix = "credential-issuer-server.status-list.status-issuer")
public class StatusIssuerProperties {

    private boolean enabled;
    private APIConnectionProperties api;

}
