package no.idporten.eudiw.statuslist.exceptions;

import org.springframework.http.HttpStatus;

public class StatusListBadRequestException extends StatusListException {
    private final String errorCode;
    private static final HttpStatus statusCode = HttpStatus.BAD_REQUEST;

    public StatusListBadRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StatusListBadRequestException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
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
