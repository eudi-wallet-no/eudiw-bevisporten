package no.idporten.eudiw.issuer.oauth2;

import no.idporten.eudiw.issuer.ErrorCode;

/**
 * Exception thrown when an access token is invalid, such as when it is expired, malformed or missing required claims.
 */
public class InvalidAccessTokenException extends UnauthorizedRequestException{

    public InvalidAccessTokenException(String errorDescription) {
        super(ErrorCode.INVALID_TOKEN, errorDescription);
    }

    public InvalidAccessTokenException(String errorDescription, Throwable cause) {
        super(ErrorCode.INVALID_TOKEN, errorDescription, cause);
    }

}
