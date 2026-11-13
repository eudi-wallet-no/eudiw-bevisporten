package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.jwt.JwtClaims;

import java.net.URI;

/**
 * Key attestation.
 */
public record KeyAttestation(SignedJWT jwt, AttestationStatus keyStorageStatus, URI certification) {

    static final String KEY_STORAGE_STATUS_CLAIM = "key_storage_status";
    static final String CERTIFICATION_CLAIM = "certification";

    /**
     * Creates key attestation with information  from attestation JWT.
     */
    public KeyAttestation(SignedJWT jwt) {
        this(jwt, JwtClaims.claimsSet(jwt));
    }

    private KeyAttestation(SignedJWT jwt, JWTClaimsSet claims) {
        this(jwt,
                AttestationStatus.fromClaims(claims, KEY_STORAGE_STATUS_CLAIM),
                JwtClaims.getURIClaim(claims, CERTIFICATION_CLAIM).orElse(null));
    }

}
