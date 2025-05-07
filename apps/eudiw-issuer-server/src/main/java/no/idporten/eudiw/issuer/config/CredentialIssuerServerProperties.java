package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@Data
@Configuration
@ConfigurationProperties(prefix = "credential-issuer-server")
public class CredentialIssuerServerProperties {

    @NotNull
    private URI credentialIssuer;

}
