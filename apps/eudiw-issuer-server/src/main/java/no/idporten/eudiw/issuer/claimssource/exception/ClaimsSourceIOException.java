package no.idporten.eudiw.issuer.claimssource.exception;

import org.springframework.http.HttpStatus;

public class ClaimsSourceIOException extends ClaimsSourceException {

    public static final String FAILED_CREDENTIAL_REQUEST = "failed_credential_request";

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    public ClaimsSourceIOException(String authoritativeSource, String errorDescription) {
        super(authoritativeSource, FAILED_CREDENTIAL_REQUEST, errorDescription, httpStatus);
    }

    public ClaimsSourceIOException(String authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, FAILED_CREDENTIAL_REQUEST, errorDescription, httpStatus, cause);
    }

}
