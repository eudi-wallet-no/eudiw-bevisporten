package no.idporten.eudiw.statuslist.exceptions;

import org.springframework.http.HttpStatus;

public class StatusListNotFoundException extends StatusListProviderException {
    private final String errorCode = ErrorCodes.STATUS_LIST_NOT_FOUND;
    private final HttpStatus statusCode = HttpStatus.NOT_FOUND;

    public StatusListNotFoundException(String id) {
        super("Could not find status list with id " + id);
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
