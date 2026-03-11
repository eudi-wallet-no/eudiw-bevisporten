package no.idporten.eudiw.connector.authoritativsources.testreg;

import no.idporten.eudiw.connector.authoritativsources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativsources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativsources.api.Subject;
import org.springframework.stereotype.Service;

@Service
public class JUnitAAuthoritativeSource implements AuthoritativeSource {
    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        return CredentialData.of(
                "identifier", subject.identifier()
        );
    }

    @Override
    public String getSource() {
        return "junit";
    }
}
