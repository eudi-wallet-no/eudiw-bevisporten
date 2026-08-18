package no.idporten.eudiw.statuslist.exceptions;

import org.springframework.http.HttpStatus;

public class UnsupportedStatusException extends StatusListException {
    private static final String errorCode = ErrorCodes.INVALID_STATUS;
    private static final HttpStatus statusCode = HttpStatus.BAD_REQUEST;

    public UnsupportedStatusException(int status) {
        super("Status %d is not supported".formatted(status));
    }

    public UnsupportedStatusException(String status) {
        super("Status '%s' is not supported".formatted(status));
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
