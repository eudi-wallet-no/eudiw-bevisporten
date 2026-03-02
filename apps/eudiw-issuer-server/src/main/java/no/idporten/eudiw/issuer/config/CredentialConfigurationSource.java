package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;

import java.util.List;
import java.util.Objects;

/**
 * Interface for sources of credential configurations.
 */
public interface CredentialConfigurationSource {

    CredentialConfigurationSourceProperties getProperties();

    /**
     * Initialize this credential configuration source.  Will be called once at startup.
     */
    void init();

    /**
     * Gets all credential configurations from this source.
     */
    List<ExtendedCredentialConfiguration> retrieve();

    /**
     * Updates the credential configurations from this source.
     */
    default void refresh() {
    }

    /**
     * Find credential configuration by credential configuration id.
     * @param credentialConfigurationId credential configuration id to find
     * @return credential configuration, or null if not found
     */
    default ExtendedCredentialConfiguration findConfiguration(String credentialConfigurationId) {
        return retrieve()
                .stream()
                .filter(c -> Objects.equals(credentialConfigurationId, c.getCredentialConfigurationId()))
                .findFirst()
                .orElse(null);
    }

}
