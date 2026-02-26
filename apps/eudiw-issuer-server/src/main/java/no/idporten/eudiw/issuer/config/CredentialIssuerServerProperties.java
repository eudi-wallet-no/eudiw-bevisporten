package no.idporten.eudiw.issuer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import org.springframework.beans.factory.InitializingBean;
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
public class CredentialIssuerServerProperties implements InitializingBean {

    @NotNull
    private URI credentialIssuer;
    @NotEmpty
    private Map<String, String> displayNames = Map.of("no", "Digitaliseringsdirektoratet");
    @NotEmpty
    private List<@NotNull String> cryptographicBindings;
    @NotEmpty
    private List<@NotNull String> proofSigningAlgorithms;
    @NotEmpty
    private List<@NotNull String> credentialSigningAlgorithms;
    @NotEmpty
    private List<@Valid AuthorizationServer> authorizationServers;
    private List<@Valid AuthorizationServer> preAuthorizationServers = new ArrayList<>();
    private List<@Valid CredentialConfigurationProperties> credentialConfigurations = new ArrayList<>();

    private List<URI> credentialConfigurationSources = new ArrayList<>();

    private CredentialIssuerContext dynamicCredentialConfigurationTemplate;

    @NotNull
    private Duration issuanceStatusPollingLifetime = Duration.ofHours(24);

    public CredentialConfigurationProperties findCredentialConfiguration(String credentialIdentifier) {
        return credentialConfigurations.stream()
                .filter(credentialConfigurationProperties -> Objects.equals(credentialIdentifier, credentialConfigurationProperties.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("unknown_credential_identifier", "Unknown credential identifier.", HttpStatus.BAD_REQUEST));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO Glue for new -> old configuration, should be removed when old configuration is removed.
        for (URI credentialConfigurationSourceUri : credentialConfigurationSources) {
            ClasspathCredentialConfigurationSource credentialConfigurationSource = new ClasspathCredentialConfigurationSource();
            List<ExtendedCredentialConfiguration> credentialConfigurations = credentialConfigurationSource.retrieve(credentialConfigurationSourceUri);
            for (ExtendedCredentialConfiguration credentialConfiguration : credentialConfigurations) {
                CredentialConfigurationProperties credentialConfigurationProperties = CredentialConfigurationProperties.builder()
                        .identifier(credentialConfiguration.getCredentialConfigurationId())
                        .credentialType(credentialConfiguration.getCredentialType())
                        .format(credentialConfiguration.getFormat())
                        .scope(credentialConfiguration.getScope())
                        .extendedCredentialMetadata(credentialConfiguration.getExtendedCredentialMetadata())
                        .claimsSourceUri(credentialConfiguration.getCredentialIssuerContext().getCredentialDataSourceUri())
                        .grantType(credentialConfiguration.getCredentialIssuerContext().getGrantType())
                        .authorizationServer(credentialConfiguration.getCredentialIssuerContext().getAuthorizationServer())
                        .preAuthorizationServer(credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationServer())
                        .preAuthorizationLifetime(credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationLifetime())
                        .keyStoreName(credentialConfiguration.getCredentialIssuerContext().getCredentialSigningKeystore())
                        .validityDays(credentialConfiguration.getCredentialIssuerContext().getValidityDays())
                        .requireTxCode(credentialConfiguration.getCredentialIssuerContext().isRequireTxCode())
                        //
                        .build();
                this.credentialConfigurations.add(credentialConfigurationProperties);
            }
        }
    }
}

