package no.idporten.eudiw.verifier.trustlist;

/**
 * How a configured trustlist document is encoded on the wire. Known and declared explicitly in
 * configuration when the trustlist is added, rather than guessed from the URL.
 */
public enum TrustlistEncoding {
    XML,
    JSON
}
