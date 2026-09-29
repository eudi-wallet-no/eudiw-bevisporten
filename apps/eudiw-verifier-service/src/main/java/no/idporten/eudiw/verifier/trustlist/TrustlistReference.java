package no.idporten.eudiw.verifier.trustlist;

import java.net.URI;

/**
 * A trustlist URL together with the format category it is known to be, based on which
 * configuration entry it was resolved from. See {@link TrustlistFormat}.
 */
public record TrustlistReference(URI uri, TrustlistFormat format) {
}
