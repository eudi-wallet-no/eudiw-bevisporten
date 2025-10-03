package no.idporten.eudiw.issuer.claimssource.exception;

import org.springframework.http.HttpStatus;

public class ClaimsSourceDataNotFoundException extends ClaimsSourceException {

    public static final String NOT_FOUND_CREDENTIAL_DATA = "credential_issuance_denied";

    private final static HttpStatus httpStatus = HttpStatus.NOT_FOUND;

    public ClaimsSourceDataNotFoundException(String authoritativeSource, String errorDescription) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, errorDescription, httpStatus);
    }

    public ClaimsSourceDataNotFoundException(String authoritativeSource, String errorDescription, String logMessage) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, errorDescription, httpStatus, logMessage);
    }

    public ClaimsSourceDataNotFoundException(String authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, errorDescription, httpStatus, cause);
    }

}
