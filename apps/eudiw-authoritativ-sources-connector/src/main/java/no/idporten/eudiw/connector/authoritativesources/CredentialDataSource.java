package no.idporten.eudiw.connector.authoritativesources;

import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;

public interface CredentialDataSource {
    CredentialData retrieveCredentialData(Subject subject);
}
