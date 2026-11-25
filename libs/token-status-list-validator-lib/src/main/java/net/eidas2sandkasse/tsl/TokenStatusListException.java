package net.eidas2sandkasse.tsl;

/**
 * Exception thrown when something fails during validation: invalid input, invalid token status list, runtime errors, ... .
 */
public class TokenStatusListException extends RuntimeException {

    public TokenStatusListException(String message) {
        super(message);
    }

    public TokenStatusListException(String message, Throwable cause) {
        super(message, cause);
    }
}
