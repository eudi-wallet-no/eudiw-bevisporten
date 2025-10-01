package no.idporten.eudiw.issuer.claimssource.exception;

import org.springframework.http.HttpStatus;

public class ClaimsSourceInvalidDataException extends ClaimsSourceException {

    public static final String FAILED_CREDENTIAL_REQUEST = "credential_issuance_denied";

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;


    public ClaimsSourceInvalidDataException(String authoritativeSource, String errorDescription) {
        super(authoritativeSource, FAILED_CREDENTIAL_REQUEST, errorDescription, httpStatus);
    }

    public ClaimsSourceInvalidDataException(String authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, FAILED_CREDENTIAL_REQUEST, errorDescription, httpStatus, cause);
    }

}
