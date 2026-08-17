package no.idporten.eudiw.connector.authoritativesources.exceptions;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.INVALID_REQUEST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class UnknownAuthoritativeSourceException extends AuthoritativeSourceException {

    public UnknownAuthoritativeSourceException(String message) {
        super(INVALID_REQUEST, message, BAD_REQUEST);
    }

}
