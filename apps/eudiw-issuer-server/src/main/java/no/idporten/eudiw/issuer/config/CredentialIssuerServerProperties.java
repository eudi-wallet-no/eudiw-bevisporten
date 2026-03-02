package no.idporten.eudiw.issuer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.configurations.CredentialIssuerContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CredentialIssuerServerProperties.class);

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
    private List<@Valid ExtendedCredentialConfiguration> credentialConfigurations = new ArrayList<>();

    private List<CredentialConfigurationSource> credentialConfigurationSources = new ArrayList<>();

    private List<CredentialConfigurationSourceProperties> credentialConfigurationSourcesProperties = new ArrayList<>();

    private CredentialIssuerContext dynamicCredentialConfigurationTemplate;

    @NotNull
    private Duration issuanceStatusPollingLifetime = Duration.ofHours(24);

    public ExtendedCredentialConfiguration findCredentialConfiguration(String credentialIdentifier) {
        return credentialConfigurationSources.stream()
                .map(ccs -> ccs.findConfiguration(credentialIdentifier))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("unknown_credential_identifier", "Unknown credential identifier.", HttpStatus.BAD_REQUEST));
    }

    /**
     * Load credential configurations from credential configuration sources.
     */
    @Override
    public void afterPropertiesSet() {
        for (CredentialConfigurationSourceProperties properties : credentialConfigurationSourcesProperties) {
            CredentialConfigurationSource credentialConfigurationSource = createCredentialConfigurationSource(properties);
            try {
                credentialConfigurationSource.init();
                log.info("Initialized credential configuration source with URI {}.", properties.uri());
            } catch (Exception e) {
                log.warn("Failed to initialize credential configuration source with URI {}.", properties.uri(), e);
            }
            this.credentialConfigurationSources.add(credentialConfigurationSource);
            this.credentialConfigurations.addAll(credentialConfigurationSource.retrieve());
        }
    }

    protected CredentialConfigurationSource createCredentialConfigurationSource(CredentialConfigurationSourceProperties properties) {
        if (properties.uri().startsWith("classpath")) {
            return new ClasspathSingleCredentialConfigurationSource(properties);
        }
        if (properties.uri().startsWith("http")) {
            return new HttpCredentialConfigurationSource(properties);
        }
        throw new IllegalArgumentException("Unsupported credential configuration source URI: %s".formatted(properties.uri()));
    }

}

