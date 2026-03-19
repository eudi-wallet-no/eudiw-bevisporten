package no.idporten.eudiw.connector.authoritativesources.freg;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

@Service
public class AgeVerificationAuthoritativeSource implements AuthoritativeSource {

    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        CredentialData credentialData = new CredentialData();
        credentialData.addBoolean("age_over_15", true);
        credentialData.addBoolean("age_over_18", false);
        return credentialData;
    }

    @Override
    public String getSource() {
        return "ageverification";
    }
}
