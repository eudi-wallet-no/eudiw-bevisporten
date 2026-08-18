package no.idporten.eudiw.issuer.oauth2;

import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;

/**
 * Exception thrown for requests that are unauthorized due to missing authorization or invalid authorization.
 */
public class UnauthorizedRequestException extends IssuerServerException {

    protected UnauthorizedRequestException(String errorDescription) {
        super(ErrorCode.UNAUTHORIZED_INVALID_REQUEST, errorDescription);
    }

    protected UnauthorizedRequestException(ErrorCode errorCode, String errorDescription) {
        super(errorCode, errorDescription);
    }

    protected UnauthorizedRequestException(ErrorCode errorCode, String errorDescription, Throwable cause) {
        super(errorCode, errorDescription, cause);
    }

}
