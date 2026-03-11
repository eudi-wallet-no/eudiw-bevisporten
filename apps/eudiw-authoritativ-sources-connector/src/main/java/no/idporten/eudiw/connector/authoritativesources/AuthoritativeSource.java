package no.idporten.eudiw.connector.authoritativesources;

import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;

public interface AuthoritativeSource {
    CredentialData retrieveCredentialData(Subject subject);
    String getSource();
}
