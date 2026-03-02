package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.IssuerServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a single credential configuration from classpath.
 */
public class ClasspathSingleCredentialConfigurationSource implements CredentialConfigurationSource {

    private static final Logger log = LoggerFactory.getLogger(ClasspathSingleCredentialConfigurationSource.class);

    private final CredentialConfigurationSourceProperties properties;
    private final List<ExtendedCredentialConfiguration> credentialConfigurations;

    public ClasspathSingleCredentialConfigurationSource(CredentialConfigurationSourceProperties properties) {
        this.properties = properties;
        this.credentialConfigurations = new ArrayList<>();
    }

    @Override
    public CredentialConfigurationSourceProperties getProperties() {
        return properties;
    }

    /**
     * Initializes and creates credential configuration only once.
     */
    @Override
    public void init() {
        JsonMapper jsonMapper = JsonMapper.builderWithJackson2Defaults().build();
        try (InputStream inputStream = new DefaultResourceLoader().getResource(properties.uri()).getInputStream()) {
            ExtendedCredentialConfiguration extendedCredentialConfiguration = jsonMapper.readValue(inputStream, ExtendedCredentialConfiguration.class);
            log.info("Retrieved credential configuration for {} from uri {}", extendedCredentialConfiguration.getCredentialType(), properties.uri());
            this.credentialConfigurations.add(extendedCredentialConfiguration);
        } catch (IOException e) {
            throw new IssuerServerException("server_error", "Failed to init credential configuration source.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public List<ExtendedCredentialConfiguration> retrieve() {
        return credentialConfigurations;
    }

}
