package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.NOT_FOUND_CREDENTIAL_DATA;

public class AuthoritativeSourceDataNotFoundException extends AuthoritativeSourceException {
    private final static HttpStatus httpStatus = HttpStatus.NOT_FOUND;

    public AuthoritativeSourceDataNotFoundException(AuthoritativeSources authoritativeSource, String message) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, message, httpStatus, null, null);
    }

    public AuthoritativeSourceDataNotFoundException(AuthoritativeSources authoritativeSource, String message, String logMessage) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, message, httpStatus, logMessage, null);
    }

}
