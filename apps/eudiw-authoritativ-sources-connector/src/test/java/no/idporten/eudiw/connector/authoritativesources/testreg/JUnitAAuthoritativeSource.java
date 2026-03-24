package no.idporten.eudiw.connector.authoritativesources.testreg;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class JUnitAAuthoritativeSource implements AuthoritativeSource {
    Map<String, CredentialDataSource> credentialDataSources;

    public JUnitAAuthoritativeSource(JUnitCredentialDataSource junitCredentialDataSource) {
        credentialDataSources = Map.of(
                JUnitCredentialDataSource.CREDENTIAL_TYPE, junitCredentialDataSource
        );
    }

    @Override
    public CredentialData retrieveCredentialData(Subject subject, String credentialType) {
        return credentialDataSources.get(credentialType).retrieveCredentialData(subject);
    }

    @Override
    public boolean isSource(String source) {
        return "junit".equals(source);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialDataSources.containsKey(credentialType);
    }
}
