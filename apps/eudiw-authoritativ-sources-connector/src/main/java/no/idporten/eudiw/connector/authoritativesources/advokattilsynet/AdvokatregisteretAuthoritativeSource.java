package no.idporten.eudiw.connector.authoritativesources.advokattilsynet;


import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

import java.util.Map;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.ADVOKATREGISTERET;

@Service
public class AdvokatregisteretAuthoritativeSource implements AuthoritativeSource {
    private final Map<String, CredentialDataSource> credentialDataSources;

    public AdvokatregisteretAuthoritativeSource(AdvokatregisteretCredentialDataSource advokatregisteretCredentialDataSource) {
        this.credentialDataSources = Map.of(
                AdvokatregisteretCredentialDataSource.CREDENTIAL_TYPE, advokatregisteretCredentialDataSource
        );
    }

    @Override
    public CredentialData retrieveCredentialData(Subject subject, String credentialType) {
        return credentialDataSources.get(credentialType).retrieveCredentialData(subject);
    }

    @Override
    public boolean isSource(String source) {
        return ADVOKATREGISTERET.externalName().equals(source);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialDataSources.containsKey(credentialType);
    }
}
