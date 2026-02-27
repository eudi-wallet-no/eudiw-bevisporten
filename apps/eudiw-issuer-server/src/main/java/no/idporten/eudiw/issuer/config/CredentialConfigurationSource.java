package no.idporten.eudiw.issuer.config;

import java.util.List;

public interface CredentialConfigurationSource {

    CredentialConfigurationSourceProperties getProperties();

    List<ExtendedCredentialConfiguration> retrieve();

    default void update() {};
}
