package no.idporten.eudiw.connector.authoritativsources.freg;

import no.idporten.eudiw.connector.authoritativsources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativsources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativsources.api.Subject;
import org.springframework.stereotype.Service;

@Service
public class AgeVerificationAuthoritativeSource implements AuthoritativeSource {

    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        return CredentialData.of(
                "age_over_15", "true",
                "age_over_18", "false"
        );
    }

    @Override
    public String getSource() {
        return "ageverification";
    }
}
