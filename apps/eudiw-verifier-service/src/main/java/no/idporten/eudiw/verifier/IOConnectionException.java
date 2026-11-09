package no.idporten.eudiw.verifier;

public class IOConnectionException extends RuntimeException {

    public IOConnectionException(String message, Exception cause) {
        super(message, cause);
    }
}
