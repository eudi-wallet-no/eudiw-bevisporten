package no.idporten.eudiw.connector.authoritativesources;

import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialDataResponse;
import no.idporten.eudiw.connector.authoritativesources.api.RetrieveRequest;
import no.idporten.eudiw.connector.authoritativesources.exceptions.UnknownAuthoritativeSourceException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthoritativeSourcesService {

    private final List<AuthoritativeSource> authoritativeSources;

    public AuthoritativeSourcesService(List<AuthoritativeSource> authoritativeSources) {
        this.authoritativeSources = authoritativeSources;
    }

    public CredentialDataResponse retrieveCredentialData(String source, RetrieveRequest request) throws UnknownAuthoritativeSourceException {
        CredentialData credentialData = getAuthoritativeSource(source, request.credentialType()).retrieveCredentialData(request.subject(),  request.credentialType());
        return new CredentialDataResponse(credentialData);
    }

    private AuthoritativeSource getAuthoritativeSource(String source, String credentialType) throws UnknownAuthoritativeSourceException {
        for (AuthoritativeSource authoritativeSource : authoritativeSources) {
            if (authoritativeSource.supports(source, credentialType)) {
                return authoritativeSource;
            }
        }
        throw new UnknownAuthoritativeSourceException("Authoritative source not found");
    }
}
