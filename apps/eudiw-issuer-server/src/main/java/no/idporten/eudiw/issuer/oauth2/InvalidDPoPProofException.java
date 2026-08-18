package no.idporten.eudiw.issuer.oauth2;

import no.idporten.eudiw.issuer.ErrorCode;

/**
 * Exception thrown when a DPoP proof token is invalid, such as when it is expired, malformed or missing required claims.
 */
public class InvalidDPoPProofException extends UnauthorizedRequestException {

    public InvalidDPoPProofException(String errorDescription, Throwable cause) {
        super(ErrorCode.INVALID_DPOP_PROOF, errorDescription, cause);
    }

}
