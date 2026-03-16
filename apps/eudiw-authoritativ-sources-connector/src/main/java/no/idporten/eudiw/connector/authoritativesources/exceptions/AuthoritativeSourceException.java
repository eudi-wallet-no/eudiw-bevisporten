package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;
import org.springframework.http.HttpStatusCode;

public class AuthoritativeSourceException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final String errorCode;
    private final AuthoritativeSources authoritativeSource;
    private final String logMessage;

    public AuthoritativeSourceException(String errorCode, String message, HttpStatusCode statusCode) {
        this(null, errorCode, message, statusCode, null, null);
    }

    public AuthoritativeSourceException(String errorCode, String message, HttpStatusCode statusCode, Throwable cause) {
        this(null, errorCode, message, statusCode, cause);
   }

   public AuthoritativeSourceException(AuthoritativeSources authoritativeSource, String errorCode, String message, HttpStatusCode statusCode, Throwable cause) {
        this(authoritativeSource, errorCode, message, statusCode, null, cause);
   }

    public AuthoritativeSourceException(AuthoritativeSources authoritativeSource, String errorCode, String message, HttpStatusCode statusCode, String logMessage) {
        this( authoritativeSource, errorCode, message, statusCode, logMessage, null);
    }

    public AuthoritativeSourceException(AuthoritativeSources authoritativeSource, String errorCode, String message, HttpStatusCode statusCode, String logMessage, Throwable cause) {
        super(message, cause);
        this.authoritativeSource = authoritativeSource;
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.logMessage = logMessage;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
