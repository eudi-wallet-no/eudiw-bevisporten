package no.idporten.eudiw.issuer.config;

import java.util.List;
import java.util.Objects;

/**
 * Interface for sources of credential configurations.
 */
public interface CredentialConfigurationSource {

    CredentialConfigurationSourceProperties getProperties();

    /**
     * Gets all credential configurations from this source.
     * @return
     */
    List<ExtendedCredentialConfiguration> retrieve();

    /**
     * Updates the credential configurations from this source.
     */
    default void update() {
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
