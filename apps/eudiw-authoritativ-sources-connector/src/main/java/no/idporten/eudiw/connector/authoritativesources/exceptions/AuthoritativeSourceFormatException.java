package no.idporten.eudiw.connector.authoritativesources.exceptions;

import org.springframework.http.HttpStatus;

public class AuthoritativeSourceFormatException extends AuthoritativeSourceException {

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    public AuthoritativeSourceFormatException(String error, String errorDescription) {
        super(error, errorDescription, httpStatus);
    }

    public AuthoritativeSourceFormatException(String error, String errorDescription, Throwable cause) {
        super(error, errorDescription, httpStatus, cause);
    }
}
