package no.idporten.eudiw.bevisgenerator.exception;

public class ByobServiceIOException extends RuntimeException {

    public ByobServiceIOException(String message) {
        super(message);
    }

     public ByobServiceIOException(String message, RuntimeException cause) {
        super(message, cause);
    }
}
