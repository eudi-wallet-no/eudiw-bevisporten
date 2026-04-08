package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.INVALID_CREDENTIAL_DATA;

public class AuthoritativeSourceInvalidDataException extends AuthoritativeSourceException {

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    public AuthoritativeSourceInvalidDataException(AuthoritativeSources authoritativeSource, String message) {
        super(authoritativeSource, INVALID_CREDENTIAL_DATA, message, httpStatus, null, null);
    }

    public AuthoritativeSourceInvalidDataException(AuthoritativeSources authoritativeSource, String error, String message, String logMessage) {
        super(authoritativeSource, error, message, httpStatus, logMessage, null);
    }

}
