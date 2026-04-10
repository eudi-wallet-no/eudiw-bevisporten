package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.SERVER_ERROR;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;


public class AuthoritativeSourceIOException extends AuthoritativeSourceException {

    public AuthoritativeSourceIOException(AuthoritativeSources authoritativeSource, String message, Throwable cause) {
        super(authoritativeSource, SERVER_ERROR, message, INTERNAL_SERVER_ERROR, cause);
    }

}
