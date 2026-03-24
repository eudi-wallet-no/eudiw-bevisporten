package no.idporten.eudiw.connector.authoritativesources.testreg;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

@Service
public class JUnitCredentialDataSource implements CredentialDataSource {
    static final String CREDENTIAL_TYPE = "junit";

    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        CredentialData credentialData = new CredentialData();
        credentialData.addString("identifier", subject.identifier());
        return credentialData;
    }
}
