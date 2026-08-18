package no.idporten.eudiw.byob.service.exception;

public class ForbiddenRequestException extends ByobServiceException {

    public ForbiddenRequestException(String message) {
        super(message);
    }
    public ForbiddenRequestException(String message, Exception e) {
        super(message, e);
    }
}
