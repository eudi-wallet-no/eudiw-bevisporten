package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.SERVER_ERROR;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class AuthoritativeSourceUnsupportedOperationException extends AuthoritativeSourceException {

    public AuthoritativeSourceUnsupportedOperationException(AuthoritativeSources authoritativeSource, String message) {
        super(authoritativeSource, SERVER_ERROR, message, INTERNAL_SERVER_ERROR, null, null);
    }
}
