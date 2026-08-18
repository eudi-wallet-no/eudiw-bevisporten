package no.idporten.eudiw.bevisgenerator.exception;

public class IssuerUiException extends RuntimeException {

    public IssuerUiException(String message) {
        super(message);
    }

    public IssuerUiException(String message, Exception cause) {
        super(message, cause);
    }

}
