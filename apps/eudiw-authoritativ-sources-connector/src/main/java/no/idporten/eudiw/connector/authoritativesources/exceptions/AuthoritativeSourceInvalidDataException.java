package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.CREDENTIAL_ISSUANCE_DENIED;

public class AuthoritativeSourceInvalidDataException extends AuthoritativeSourceException {

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    public AuthoritativeSourceInvalidDataException(AuthoritativeSources authoritativeSource, String errorDescription) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus, null, null);
    }

    public AuthoritativeSourceInvalidDataException(AuthoritativeSources authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus, cause);
    }

    public AuthoritativeSourceInvalidDataException(AuthoritativeSources authoritativeSource, String error, String errorDescription, String logMessage) {
        super(authoritativeSource, error, errorDescription, httpStatus, logMessage, null);
    }

}
