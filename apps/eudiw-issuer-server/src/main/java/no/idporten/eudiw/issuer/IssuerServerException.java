package no.idporten.eudiw.issuer;

import org.springframework.http.HttpStatus;

public class IssuerServerException extends RuntimeException {

    private final String error;
    private final String logMessage;
    private final HttpStatus httpStatus;

    public IssuerServerException(ErrorCode errorCode, String errorDescription) {
        this(errorCode, errorDescription, null, null);
    }

    public IssuerServerException(ErrorCode errorCode, String errorDescription, Throwable cause) {
        this(errorCode, errorDescription, null, cause);
    }

    public IssuerServerException(ErrorCode errorCode, String errorDescription, String logMessage) {
        this(errorCode, errorDescription, logMessage, null);
    }

    protected IssuerServerException(ErrorCode errorCode, String errorDescription, String logMessage, Throwable cause) {
        super(errorDescription, cause);
        this.error = errorCode.error();
        this.httpStatus = errorCode.httpStatus();
        this.logMessage = logMessage;
    }

    public String getError() {
        return error;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorDescription() {
        return super.getMessage();
    }

}
