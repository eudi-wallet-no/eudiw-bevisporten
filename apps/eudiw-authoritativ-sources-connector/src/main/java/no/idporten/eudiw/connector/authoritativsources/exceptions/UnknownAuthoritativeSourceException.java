package no.idporten.eudiw.connector.authoritativsources.exceptions;

import org.springframework.http.HttpStatus;

import static no.idporten.eudiw.connector.authoritativsources.exceptions.ErrorCodes.INVALID_REQUEST;

public class UnknownAuthoritativeSourceException extends AuthoritativeSourceException {
    public UnknownAuthoritativeSourceException(String message) {
        super(INVALID_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
