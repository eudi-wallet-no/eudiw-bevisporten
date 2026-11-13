package no.idporten.eudiw.issuer.jwt;

/**
 * Thrown when a claim is present in a JWT, but does not have the expected type or structure.
 */
public class InvalidClaimException extends RuntimeException {

    public InvalidClaimException(String message) {
        super(message);
    }

    public InvalidClaimException(String message, Throwable cause) {
        super(message, cause);
    }

}
