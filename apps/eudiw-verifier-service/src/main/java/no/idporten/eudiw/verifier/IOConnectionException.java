package no.idporten.eudiw.verifier;

import org.springframework.http.HttpStatus;

public class IOConnectionException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public IOConnectionException(String errorCode, String errorDescription, Exception exception) {
        super(errorDescription, exception);
        if (errorCode == null || errorCode.isBlank()) {
            this.errorCode = ErrorCode.SERVER_ERROR.error();
        } else {
            this.errorCode = errorCode;
        }
        this.httpStatus = ErrorCode.httpStatusFor(this.errorCode);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return getMessage();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

}
