package no.idporten.eudiw.connector.authoritativesources.exceptions;

import org.springframework.http.HttpStatus;

public class ClaimsSourceFormatException extends AuthoritativeSourceException {

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    public ClaimsSourceFormatException(String error, String errorDescription) {
        super(error, errorDescription, httpStatus);
    }

    public ClaimsSourceFormatException(String error, String errorDescription, Throwable cause) {
        super(error, errorDescription, httpStatus, cause);
    }
}
