package net.eidas2sandkasse.trustlist;

/**
 * The status of the trustlist check could not be determined: the list could not be fetched, invalid signature etc.
 */
public class TrustlistException extends RuntimeException {

    public TrustlistException(String message, Throwable cause) {
        super(message, cause);
    }
}
