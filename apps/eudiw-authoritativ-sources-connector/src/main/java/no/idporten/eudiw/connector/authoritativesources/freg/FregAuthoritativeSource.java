package no.idporten.eudiw.connector.authoritativesources.freg;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

import java.util.Map;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.FREG;

@Service
public class FregAuthoritativeSource implements AuthoritativeSource {
    private final Map<String, CredentialDataSource> credentialDataSources;

    public FregAuthoritativeSource(AgeVerificationCredentialDataSource ageVerificationCredentialDataSource, PidCredentialDataSource pidCredentialDataSource) {
        credentialDataSources = Map.of(
                AgeVerificationCredentialDataSource.CREDENTIAL_TYPE, ageVerificationCredentialDataSource,
                PidCredentialDataSource.CREDENTIAL_TYPE, pidCredentialDataSource
        );
    }


    @Override
    public CredentialData retrieveCredentialData(Subject subject, String credentialType) {
        return credentialDataSources.get(credentialType).retrieveCredentialData(subject);
    }

    @Override
    public boolean isSource(String source) {
        return FREG.externalName().equals(source);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialDataSources.containsKey(credentialType);
    }
}
