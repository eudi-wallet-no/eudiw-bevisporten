package no.idporten.eudiw.connector.authoritativesources.freg;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
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
