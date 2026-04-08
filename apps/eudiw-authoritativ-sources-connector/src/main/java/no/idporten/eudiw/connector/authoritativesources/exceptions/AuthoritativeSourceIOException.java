package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.FAILED_CREDENTIAL_REQUEST;

public class AuthoritativeSourceIOException extends AuthoritativeSourceException {

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    public AuthoritativeSourceIOException(AuthoritativeSources authoritativeSource, String message, Throwable cause) {
        super(authoritativeSource, FAILED_CREDENTIAL_REQUEST, message, httpStatus, cause);
    }

}
