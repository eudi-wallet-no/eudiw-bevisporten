package no.idporten.eudiw.connector.authoritativesources.krr;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

import java.util.Map;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.KRR;

@Service
public class KrrAuthoritativeSource implements AuthoritativeSource {
    private final Map<String, CredentialDataSource> credentialDataSources;

    public KrrAuthoritativeSource(KrrCredentialDataSource krrCredentialDataSource) {
        credentialDataSources = Map.of(
                KrrCredentialDataSource.CREDENTIAL_TYPE, krrCredentialDataSource
        );
    }

    @Override
    public CredentialData retrieveCredentialData(Subject subject, String credentialType) {
        return credentialDataSources.get(credentialType).retrieveCredentialData(subject);
    }

    @Override
    public boolean isSource(String source) {
        return KRR.externalName().equals(source);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialDataSources.containsKey(credentialType);
    }
}
