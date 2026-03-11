package no.idporten.eudiw.connector.authoritativesources.krr;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

@Service
public class KrrAuthoritativeSource implements AuthoritativeSource {

    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        return CredentialData.of(
                "personidentifikator", subject.identifier(),
                "mobiltelefonnummer", "99887766",
                "epostadresse", "navn@domene.no"
        );
    }

    @Override
    public String getSource() {
        return "krr";
    }
}
