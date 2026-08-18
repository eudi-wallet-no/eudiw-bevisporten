package no.idporten.eudiw.issuer.claimssource.exception;

import no.idporten.eudiw.issuer.IssuerServerException;

import static no.idporten.eudiw.issuer.ErrorCode.CREDENTIAL_REQUEST_DENIED;

public class StatusListException extends IssuerServerException {
    public StatusListException(String errorDescription, String logMessage) {
        super(CREDENTIAL_REQUEST_DENIED, errorDescription, logMessage);
    }

    public StatusListException(String errorDescription, String logMessage, Throwable cause) {
        super(CREDENTIAL_REQUEST_DENIED, errorDescription, logMessage, cause);
    }
}
