package no.idporten.eudiw.byob.service.exception;

public class BadRequestException extends ByobServiceException {

    public BadRequestException(String message) {
        super(message);
    }
    public BadRequestException(String message, Exception e) {
        super(message, e);
    }
}
