package no.idporten.eudiw.statuslist.exceptions;

import org.springframework.http.HttpStatus;

public class StatusNotAllocatedException extends StatusListException {
    private static final String errorCode = ErrorCodes.STATUS_NOT_ALLOCATED;
    private static final HttpStatus statusCode = HttpStatus.BAD_REQUEST;

    public StatusNotAllocatedException(String id, int index) {
        super("Status at index %d is not allocated in status list with id %s".formatted(index, id));
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
