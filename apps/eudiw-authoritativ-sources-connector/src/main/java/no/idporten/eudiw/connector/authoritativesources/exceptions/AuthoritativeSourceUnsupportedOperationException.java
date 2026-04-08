package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.NOT_SUPPORTED;

public class AuthoritativeSourceUnsupportedOperationException extends AuthoritativeSourceException {

    private final static HttpStatus httpStatus = HttpStatus.NOT_IMPLEMENTED;

    public AuthoritativeSourceUnsupportedOperationException(AuthoritativeSources authoritativeSource, String message) {
        super(authoritativeSource, NOT_SUPPORTED, message, httpStatus, null, null);
    }
}
