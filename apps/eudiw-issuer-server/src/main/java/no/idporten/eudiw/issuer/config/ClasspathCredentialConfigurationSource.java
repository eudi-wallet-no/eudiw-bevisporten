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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads credential configurations from classpath.
 */
public class ClasspathCredentialConfigurationSource implements CredentialConfigurationSource {

    private static final Logger log = LoggerFactory.getLogger(ClasspathCredentialConfigurationSource.class);

    private final LocalResourceProperties properties;
    private final List<ExtendedCredentialConfiguration> credentialConfigurations;

    public ClasspathCredentialConfigurationSource(LocalResourceProperties properties) {
        this.properties = properties;
        this.credentialConfigurations = new ArrayList<>();
    }

    /**
     * Initializes and creates credential configuration only once.
     */
    @Override
    public void init() {
        for (URI path : properties.paths()) {
            this.credentialConfigurations.add(readExtendedCredentialConfiguration(path));
        }
    }

    protected ExtendedCredentialConfiguration readExtendedCredentialConfiguration(URI uri) {
        JsonMapper jsonMapper = JsonMapper.builderWithJackson2Defaults().build();
        try (InputStream inputStream = new DefaultResourceLoader().getResource(uri.toString()).getInputStream()) {
            ExtendedCredentialConfiguration credentialConfiguration = jsonMapper.readValue(inputStream, ExtendedCredentialConfiguration.class);
            log.info("Retrieved credential configuration for {} from uri {}", credentialConfiguration.getCredentialType(), uri);
            return credentialConfiguration;
        } catch (IOException e) {
            throw new IssuerServerException(ErrorCode.SERVER_ERROR, "Failed to init credential configuration source.", e);
        }
    }

    @Override
    public List<ExtendedCredentialConfiguration> retrieve() {
        return credentialConfigurations;
    }

}
