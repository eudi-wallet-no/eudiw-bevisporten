package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.NOT_FOUND_CREDENTIAL_DATA;

public class ClaimsSourceDataNotFoundException extends AuthoritativeSourceException {
    private final static HttpStatus httpStatus = HttpStatus.NOT_FOUND;

    public ClaimsSourceDataNotFoundException(AuthoritativeSources authoritativeSource, String errorDescription) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, errorDescription, httpStatus, null, null);
    }

    public ClaimsSourceDataNotFoundException(AuthoritativeSources authoritativeSource, String errorDescription, String logMessage) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, errorDescription, httpStatus, logMessage, null);
    }

    public ClaimsSourceDataNotFoundException(AuthoritativeSources authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, errorDescription, httpStatus, cause);
    }
}
