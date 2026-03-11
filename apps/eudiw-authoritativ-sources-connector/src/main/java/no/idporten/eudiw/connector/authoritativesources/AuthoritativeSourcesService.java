package no.idporten.eudiw.connector.authoritativesources;

import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialDataResponse;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.UnknownAuthoritativeSourceException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthoritativeSourcesService {

    private final List<AuthoritativeSource> authoritativeSources;

    public AuthoritativeSourcesService(List<AuthoritativeSource> authoritativeSources) {
        this.authoritativeSources = authoritativeSources;
    }

    public CredentialDataResponse retrieveCredentialData(String source, Subject subject) throws UnknownAuthoritativeSourceException {
        CredentialData credentialData = getAuthoritativeSource(source).retrieveCredentialData(subject);
        return new CredentialDataResponse(credentialData);
    }

    private AuthoritativeSource  getAuthoritativeSource(String source) throws UnknownAuthoritativeSourceException {
        for (AuthoritativeSource authoritativeSource : authoritativeSources) {
            if (authoritativeSource.getSource().equals(source)) {
                return authoritativeSource;
            }
        }
        throw new UnknownAuthoritativeSourceException("No AuthoritativeSource found for given source");
    }
}
