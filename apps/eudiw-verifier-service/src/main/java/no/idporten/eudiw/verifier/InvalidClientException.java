package no.idporten.eudiw.verifier;

public class InvalidClientException extends VerificationException {

    public InvalidClientException(String errorDescription, String logMessage) {
        super(ErrorCode.INVALID_CLIENT, errorDescription, logMessage);
    }
}
