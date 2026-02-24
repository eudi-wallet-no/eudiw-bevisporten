package no.idporten.eudiw.issuer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Validated
@Data
@Configuration
@ConfigurationProperties(prefix = "credential-issuer-server")
public class CredentialIssuerServerProperties {

    @NotNull
    private URI credentialIssuer;
    @NotEmpty
    private Map<String, String> displayNames = Map.of("no", "Digitaliseringsdirektoratet");
    @NotEmpty
    private List<@NotNull String> formats;
    @NotEmpty
    private List<@NotNull String> cryptographicBindings;
    @NotEmpty
    private List<@NotNull String> proofSigningAlgorithms;
    @NotEmpty
    private List<@NotNull String> credentialSigningAlgorithms;
    @NotEmpty
    private List<AuthorizationServer> authorizationServers;
    private List<AuthorizationServer> preAuthorizationServers = new ArrayList<>();
    @NotEmpty
    private List<@Valid CredentialConfigurationProperties> credentialConfigurations;

    private CredentialConfigurationProperties dynamicCredentialConfigurationTemplate;

    @NotNull
    private Duration issuanceStatusPollingLifetime = Duration.ofHours(24);

    public CredentialConfigurationProperties findCredentialConfiguration(String credentialIdentifier) {
        List<CredentialIssuerServerProperties> allCredentialConfigurations = new ArrayList<>();
        return credentialConfigurations.stream()
                .filter(credentialConfigurationProperties -> Objects.equals(credentialIdentifier, credentialConfigurationProperties.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("unknown_credential_identifier", "Unknown credential identifier.", HttpStatus.BAD_REQUEST));
    }

}
