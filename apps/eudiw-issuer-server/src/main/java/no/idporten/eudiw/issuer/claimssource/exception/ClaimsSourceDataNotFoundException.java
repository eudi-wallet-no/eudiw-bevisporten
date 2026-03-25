package no.idporten.eudiw.issuer.claimssource.exception;

import org.springframework.http.HttpStatus;

public class ClaimsSourceDataNotFoundException extends ClaimsSourceException {

    public static final String CREDENTIAL_ISSUANCE_DENIED = "credential_issuance_denied";

    private final static HttpStatus httpStatus = HttpStatus.NOT_FOUND;

    public ClaimsSourceDataNotFoundException(String authoritativeSource, String errorDescription) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus);
    }

    public ClaimsSourceDataNotFoundException(String authoritativeSource, String errorDescription, String logMessage) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus, logMessage);
    }

    public ClaimsSourceDataNotFoundException(String authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus, cause);
    }

}
