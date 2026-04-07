package no.idporten.eudiw.issuer.claimssource.exception;

import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;

public class ClaimsSourceException extends IssuerServerException {

    /** The authoritative source that caused the I/O error */
    private final String authoritativeSource;
    private String logMessage;

    public ClaimsSourceException(String authoritativeSource, String error, String errorDescription, HttpStatus httpStatus) {
        super(error, errorDescription, httpStatus, null);
        this.authoritativeSource = authoritativeSource;
    }
    public ClaimsSourceException(String authoritativeSource, String error, String errorDescription, HttpStatus httpStatus, String logMessage) {
        super(error, errorDescription, httpStatus, null);
        this.authoritativeSource = authoritativeSource;
        this.logMessage = logMessage;
    }

    public ClaimsSourceException(String authoritativeSource, String error, String errorDescription, HttpStatus httpStatus, Throwable cause) {
        super(error, errorDescription, httpStatus, cause);
        this.authoritativeSource = authoritativeSource;
    }

    public String getAuthoritativeSource() {
        return authoritativeSource;
    }

    public String getLogMessage() {
        return logMessage;
    }
}
