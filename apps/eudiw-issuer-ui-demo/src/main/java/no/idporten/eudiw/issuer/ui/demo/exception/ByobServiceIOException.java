package no.idporten.eudiw.issuer.ui.demo.exception;

public class ByobServiceIOException extends RuntimeException {

    public ByobServiceIOException(String message) {
        super(message);
    }

     public ByobServiceIOException(String message, RuntimeException cause) {
        super(message, cause);
    }
}
