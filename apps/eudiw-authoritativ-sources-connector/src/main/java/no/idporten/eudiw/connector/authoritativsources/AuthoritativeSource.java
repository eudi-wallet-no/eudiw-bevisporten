package no.idporten.eudiw.connector.authoritativsources;

import no.idporten.eudiw.connector.authoritativsources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativsources.api.Subject;

public interface AuthoritativeSource {
    CredentialData retrieveCredentialData(Subject subject);
    String getSource();
}
