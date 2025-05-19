package no.idporten.eudiw.issuer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Validated
@Data
@Configuration
@ConfigurationProperties(prefix = "credential-issuer-server")
public class CredentialIssuerServerProperties {

    @NotNull
    private URI credentialIssuer;
    @NotEmpty
    private List<@NotNull String> formats;
    @NotEmpty
    private List<@NotNull String> cryptographicBindings;
    @NotEmpty
    private List<URI> authorizationServers;
    @NotEmpty
    private List<@Valid CredentialConfigurationProperties> credentialConfigurations;
    @NotEmpty
    private List<ClaimsSourceProperties> claimsSources;

    public CredentialConfigurationProperties findCredentialConfiguration(String credentialIdentifier) {
        return credentialConfigurations.stream()
                .filter(credentialConfigurationProperties -> Objects.equals(credentialIdentifier, credentialConfigurationProperties.getIdentifier()))
                .findFirst()
                .orElse(null);
    }

}
