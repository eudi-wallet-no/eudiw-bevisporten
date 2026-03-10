package no.idporten.eudiw.issuer;

import org.springframework.http.HttpStatus;

public class IssuerServerException extends RuntimeException {

    public static final String INVALID_CREDENTIAL_REQUEST = "invalid_credential_request";

    private final String error;
    private final String logMessage;
    private final HttpStatus httpStatus;

    public IssuerServerException(String error, String errorDescription, HttpStatus httpStatus) {
        this(error, errorDescription, null, httpStatus, null);
    }

    public IssuerServerException(String error, String errorDescription, HttpStatus httpStatus, Throwable cause) {
        this(error, errorDescription, null, httpStatus, cause);
    }

    public IssuerServerException(String error, String errorDescription, String logMessage, HttpStatus httpStatus) {
        this(error, errorDescription, logMessage, httpStatus, null);
    }

    public IssuerServerException(String error, String errorDescription, String logMessage, HttpStatus httpStatus, Throwable cause) {
        super(errorDescription, cause);
        this.error = error;
        this.logMessage = logMessage;
        this.httpStatus = httpStatus;
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
