package no.idporten.eudiw.issuer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@Validated
@Data
@Configuration
@ConfigurationProperties(prefix = "credential-issuer-server")
public class CredentialIssuerServerProperties implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(CredentialIssuerServerProperties.class);

    @NotNull
    private URI credentialIssuer;
    @NotEmpty
    private List<@NotNull String> cryptographicBindings;
    @NotEmpty
    private List<@NotNull String> proofSigningAlgorithms;
    @NotEmpty
    private List<@NotNull String> credentialSigningAlgorithms;
    @NotEmpty
    private List<@Valid AuthorizationServer> authorizationServers;
    private List<@Valid AuthorizationServer> preAuthorizationServers = new ArrayList<>();
    private Map<String, @Valid AuthoritativeSourceProperties> authoritativeSources = new HashMap<>();
    private Map<String, CredentialIssuerTenant> tenants = new HashMap<>();

    @NotNull
    private Duration issuanceStatusPollingLifetime = Duration.ofHours(24);

    /**
     * Load credential configurations from credential configuration sources.
     */
    @Override
    public void afterPropertiesSet() {
        for (CredentialIssuerTenant tenant : tenants.values()) {
            for (CredentialConfigurationSourceProperties properties : tenant.getCredentialConfigurationSourcesProperties()) {
                CredentialConfigurationSource credentialConfigurationSource = createCredentialConfigurationSource(properties);
                try {
                    credentialConfigurationSource.init();
                    log.info("Initialized credential configuration source with URI {}.", properties.api().uri());
                } catch (Exception e) {
                    log.warn("Failed to initialize credential configuration source with URI {}.", properties.api().uri(), e);
                }
                tenant.getCredentialConfigurationSources().add(credentialConfigurationSource);
            }
        }
    }

    protected CredentialConfigurationSource createCredentialConfigurationSource(CredentialConfigurationSourceProperties properties) {
        if (Set.of("file", "jar", "classpath").contains(properties.api().uri().getScheme())) {
            return new ClasspathSingleCredentialConfigurationSource(properties);
        }
        if (Set.of("http", "https").contains(properties.api().uri().getScheme())) {
            return new HttpCredentialConfigurationSource(properties);
        }
        throw new IllegalArgumentException("Unsupported credential configuration source URI: %s".formatted(properties.api().uri()));
    }

}

