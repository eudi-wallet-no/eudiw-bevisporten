package no.idporten.eudiw.issuer.ui.demo.exception;

public class IssuerUiException extends RuntimeException {

    public IssuerUiException(String message) {
        super(message);
    }

    public IssuerUiException(String message, Exception cause) {
        super(message, cause);
    }

}
