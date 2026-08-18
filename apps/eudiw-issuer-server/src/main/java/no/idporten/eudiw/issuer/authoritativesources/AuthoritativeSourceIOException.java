package no.idporten.eudiw.issuer.authoritativesources;

import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;

public class AuthoritativeSourceIOException extends CredentialRequestDeniedException {

    public AuthoritativeSourceIOException(String authoritativeSource, String errorDescription, String logMessage) {
        super(authoritativeSource, errorDescription, logMessage);
    }

    public AuthoritativeSourceIOException(String authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, errorDescription, cause);
    }
}
