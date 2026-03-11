package no.idporten.eudiw.connector.authoritativsources.krr;

import no.idporten.eudiw.connector.authoritativsources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativsources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativsources.api.Subject;
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
