package no.idporten.eudiw.issuer.claimssource.exception;

import org.springframework.http.HttpStatus;


// TODO egen sak senere når mer kode er borte - endre navn og se over hvilke exceptions som trengs
/**
 * Credential issuance denied due to invalid data.
 * See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-error-response
 */
public class ClaimsSourceInvalidDataException extends ClaimsSourceException {

    public static final String CREDENTIAL_ISSUANCE_DENIED = "credential_issuance_denied";

    private final static HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    public ClaimsSourceInvalidDataException(String authoritativeSource, String errorDescription) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus);
    }

    public ClaimsSourceInvalidDataException(String authoritativeSource, String errorDescription, Throwable cause) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus, cause);
    }

    public ClaimsSourceInvalidDataException(String authoritativeSource, String errorDescription, String logMessage) {
        super(authoritativeSource, CREDENTIAL_ISSUANCE_DENIED, errorDescription, httpStatus, logMessage);
    }

}
