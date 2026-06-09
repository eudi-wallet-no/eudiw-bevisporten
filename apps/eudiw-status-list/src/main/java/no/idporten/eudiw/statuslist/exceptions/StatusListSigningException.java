package no.idporten.eudiw.statuslist.exceptions;

import org.springframework.http.HttpStatus;

public class StatusListSigningException extends StatusListException {
    private static final String errorCode = ErrorCodes.SERVER_ERROR;
    private static final HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;

    public StatusListSigningException(String message) {
        super(message);
    }

    public StatusListSigningException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
