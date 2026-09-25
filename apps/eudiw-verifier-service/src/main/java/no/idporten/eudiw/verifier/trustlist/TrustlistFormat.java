package no.idporten.eudiw.verifier.trustlist;

/**
 * The concrete document format of a trustlist, combining its ETSI TS category with its wire
 * encoding. The category (612 vs 602) is determined by where a trustlist URL is configured:
 * entries under {@code trustlists.attestations} and the {@code default} entry under
 * {@code trustlists.issuer-trustlists} are always ETSI TS 119 612, which is always XML. Entries
 * under {@code trustlists.pid} and any other (non-default) entry under
 * {@code trustlists.issuer-trustlists} are always ETSI TS 119 602, which may be delivered either
 * as a signed XML document or as JSON inside a JWS/JWT; for those, the encoding is declared
 * explicitly in configuration via {@link TrustlistEncoding} rather than guessed from the URL.
 */
public enum TrustlistFormat {
    ETSI_612_XML,
    ETSI_602_XML,
    ETSI_602_JSON
}
