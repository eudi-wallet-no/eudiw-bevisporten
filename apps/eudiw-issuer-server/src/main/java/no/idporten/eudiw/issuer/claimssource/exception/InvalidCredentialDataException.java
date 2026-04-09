package no.idporten.eudiw.issuer.claimssource.exception;

import no.idporten.eudiw.issuer.IssuerServerException;

import static no.idporten.eudiw.issuer.claimssource.exception.ErrorCode.INVALID_REQUEST;

/**
 * Exception thrown when credential data is invalid.
 */
public class InvalidCredentialDataException extends IssuerServerException {

    public InvalidCredentialDataException(String errorDescription) {
        super(INVALID_REQUEST, errorDescription);
    }

    public InvalidCredentialDataException(String errorDescription, Throwable cause) {
        super(INVALID_REQUEST, errorDescription, cause);
    }

}
