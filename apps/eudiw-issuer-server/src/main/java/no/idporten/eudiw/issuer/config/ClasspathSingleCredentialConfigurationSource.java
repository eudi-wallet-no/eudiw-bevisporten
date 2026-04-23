package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
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
        try (InputStream inputStream = new DefaultResourceLoader().getResource(properties.api().uri().toString()).getInputStream()) {
            ExtendedCredentialConfiguration extendedCredentialConfiguration = jsonMapper.readValue(inputStream, ExtendedCredentialConfiguration.class);
            log.info("Retrieved credential configuration for {} from uri {}", extendedCredentialConfiguration.getCredentialType(), properties.api().uri());
            this.credentialConfigurations.add(extendedCredentialConfiguration);
        } catch (IOException e) {
            throw new IssuerServerException(ErrorCode.SERVER_ERROR, "Failed to init credential configuration source.", e);
        }
    }

    @Override
    public List<ExtendedCredentialConfiguration> retrieve() {
        return credentialConfigurations;
    }

}
