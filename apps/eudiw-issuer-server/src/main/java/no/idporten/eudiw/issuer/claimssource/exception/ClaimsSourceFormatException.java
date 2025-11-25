package no.idporten.eudiw.issuer.claimssource.exception;

import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;

public class ClaimsSourceFormatException extends IssuerServerException {

    private final static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
    public final static String INVALID_CLAIMS_DATA = "invalid_claims_data";

    public ClaimsSourceFormatException(String error, String errorDescription) {
        super(error, errorDescription, httpStatus);
    }

    public ClaimsSourceFormatException(String error, String errorDescription, Throwable cause) {
        super(error, errorDescription, httpStatus, cause);
    }
}
