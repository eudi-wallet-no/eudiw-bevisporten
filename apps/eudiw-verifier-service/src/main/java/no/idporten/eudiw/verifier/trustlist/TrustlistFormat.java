package no.idporten.eudiw.verifier.trustlist;

/**
 * The concrete document format of a trustlist, combining its ETSI TS category with its wire
 * encoding. The category (612 vs 602) is determined by where a trustlist is referenced in
 * configuration: lists under {@code trustlists.attestations} are always ETSI TS 119 612, which is
 * always XML, while lists under {@code trustlists.pid} are always ETSI TS 119 602, which may be
 * delivered either as a signed XML document or as JSON inside a JWS/JWT. For the latter, the
 * encoding is declared explicitly per entry via {@link TrustlistEncoding} rather than guessed
 * from the URL.
 */
public enum TrustlistFormat {
    ETSI_612_XML,
    ETSI_602_XML,
    ETSI_602_JSON
}
