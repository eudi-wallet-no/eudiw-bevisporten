package no.idporten.eudiw.statuslist.exceptions;

import org.springframework.http.HttpStatus;

public class StatusListProviderException extends RuntimeException {
    private final String errorCode = ErrorCodes.SERVER_ERROR;
    private final HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;

    public StatusListProviderException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
