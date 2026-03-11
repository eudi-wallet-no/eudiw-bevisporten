package no.idporten.eudiw.connector.authoritativsources.exceptions;

import org.springframework.http.HttpStatusCode;

public class AuthoritativeSourceException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final String errorCode;

    public AuthoritativeSourceException(String errorCode, String message, HttpStatusCode statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public AuthoritativeSourceException(String errorCode, String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
