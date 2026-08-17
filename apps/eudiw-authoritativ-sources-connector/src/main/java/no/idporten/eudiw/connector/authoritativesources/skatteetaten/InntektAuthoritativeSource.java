package no.idporten.eudiw.connector.authoritativesources.skatteetaten;


import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

import java.util.Map;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.SKATTEETATEN;


@Service
public class InntektAuthoritativeSource implements AuthoritativeSource {
    private final Map<String, CredentialDataSource> credentialDataSources;

    public InntektAuthoritativeSource(InntektCredentialDataSource inntektCredentialDataSource) {
        this.credentialDataSources = Map.of(
                InntektCredentialDataSource.CREDENTIAL_TYPE, inntektCredentialDataSource
        );
    }

    @Override
    public CredentialData retrieveCredentialData(Subject subject, String credentialType) {
        return credentialDataSources.get(credentialType).retrieveCredentialData(subject);
    }

    @Override
    public boolean isSource(String source) {
        return SKATTEETATEN.externalName().equals(source);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialDataSources.containsKey(credentialType);
    }
}
