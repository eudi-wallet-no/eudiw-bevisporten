package no.idporten.eudiw.byob.service.exception;

public class ByobServiceException extends RuntimeException {

    public ByobServiceException(String message) {
        super(message);
    }
    public ByobServiceException(String message, Exception e) {
        super(message, e);
    }
}
