package no.idporten.eudiw.verifier;

public class StatusCommunicationException extends VerificationException {

    public StatusCommunicationException(String errorDescription, String logMessage, Exception e) {
        super(ErrorCode.INVALID_REQUEST, errorDescription, logMessage, e);
    }
}
