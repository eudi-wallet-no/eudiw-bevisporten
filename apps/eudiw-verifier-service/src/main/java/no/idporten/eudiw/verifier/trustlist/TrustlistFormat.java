package no.idporten.eudiw.verifier.trustlist;

/**
 * Trustlist format/category. This is determined by where a trustlist URL is configured, not by
 * sniffing the URL's file extension: entries under {@code trustlists.attestations} and the
 * {@code default} entry under {@code trustlists.issuer-trustlists} are always ETSI TS 119 612 XML,
 * while entries under {@code trustlists.pid} and any other (non-default) entry under
 * {@code trustlists.issuer-trustlists} are always ETSI TS 119 602, delivered either as a signed XML
 * document or as JSON inside a JWS/JWT.
 */
public enum TrustlistFormat {
    ETSI_612_XML,
    ETSI_602
}
