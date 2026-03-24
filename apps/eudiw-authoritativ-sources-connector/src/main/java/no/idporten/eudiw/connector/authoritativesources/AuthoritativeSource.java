package no.idporten.eudiw.connector.authoritativesources;

import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;

public interface AuthoritativeSource {
    CredentialData retrieveCredentialData(Subject subject, String credentialType);

    boolean isSource(String source);

    boolean supportsCredentialType(String credentialType);

    default boolean supports(String source, String credentialType) {
        return isSource(source) && supportsCredentialType(credentialType);
    }
}
