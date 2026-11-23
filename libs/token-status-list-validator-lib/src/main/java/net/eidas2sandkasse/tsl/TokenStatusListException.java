package net.eidas2sandkasse.tsl;

/**
 * The status could not be determined: the list could not be fetched, the signature is invalid, the token is
 * expired, or the index is out of range.
 */
public class TokenStatusListException extends RuntimeException {

    public TokenStatusListException(String message, Throwable cause) {
        super(message, cause);
    }
}
