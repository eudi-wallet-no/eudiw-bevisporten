package no.idporten.eudiw.statuslist.exceptions;

import org.springframework.http.HttpStatus;

public class StatusListException extends RuntimeException {
    private static final String errorCode = ErrorCodes.SERVER_ERROR;
    private static final HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;

    public StatusListException(String message) {
        super(message);
    }

    public StatusListException(String message, Throwable cause) {
        super(message , cause);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
