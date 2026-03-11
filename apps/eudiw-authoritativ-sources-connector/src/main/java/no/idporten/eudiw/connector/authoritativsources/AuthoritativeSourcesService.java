package no.idporten.eudiw.connector.authoritativsources;

import no.idporten.eudiw.connector.authoritativsources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativsources.api.CredentialDataResponse;
import no.idporten.eudiw.connector.authoritativsources.api.Subject;
import no.idporten.eudiw.connector.authoritativsources.exceptions.UnknownAuthoritativeSourceException;
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
