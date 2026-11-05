package no.idporten.eudiw.verifier;

import org.springframework.http.HttpStatus;

public class IOConnectionException extends RuntimeException {

    private final String errorCode;
    private final String errorDescription;
    private final HttpStatus httpStatus;

    public IOConnectionException(String errorCode, String errorDescription, Exception exception) {
        super(errorDescription, exception);
        this.errorCode = errorCode == null || errorCode.isBlank() ? ErrorCode.SERVER_ERROR.error() : errorCode;
        this.errorDescription = errorDescription;
        this.httpStatus = ErrorCode.httpStatusFor(this.errorCode);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

}
