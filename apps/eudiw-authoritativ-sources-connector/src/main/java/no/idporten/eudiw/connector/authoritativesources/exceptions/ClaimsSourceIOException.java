package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.FAILED_CREDENTIAL_REQUEST;

public class ClaimsSourceIOException extends AuthoritativeSourceException {

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    public ClaimsSourceIOException(AuthoritativeSources authoritativeSource, String errorDescription) {
        super(authoritativeSource, FAILED_CREDENTIAL_REQUEST, errorDescription, httpStatus, null, null);
    }

    public ClaimsSourceIOException(AuthoritativeSources authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, FAILED_CREDENTIAL_REQUEST, errorDescription, httpStatus, cause);
    }

}
