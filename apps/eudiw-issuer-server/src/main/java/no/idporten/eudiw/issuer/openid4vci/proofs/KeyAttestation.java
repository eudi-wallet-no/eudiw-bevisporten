package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.jwt.InvalidClaimException;

import java.text.ParseException;

/**
 * Key attestation.
 */
public record KeyAttestation(SignedJWT jwt, AttestationStatus keyStorageStatus) {

    static final String KEY_STORAGE_STATUS_CLAIM = "key_storage_status";

    /**
     * Creates key attestation with key storage status from attestation.
     *
     * @throws InvalidClaimException if the claims or the key_storage_status claim of the jwt are invalid
     */
    public KeyAttestation(SignedJWT jwt) {
        this(jwt, readKeyStorageStatus(jwt));
    }

    private static AttestationStatus readKeyStorageStatus(SignedJWT jwt) {
        try {
            return AttestationStatus.fromClaims(jwt.getJWTClaimsSet(), KEY_STORAGE_STATUS_CLAIM);
        } catch (ParseException e) {
            throw new InvalidClaimException("Invalid claims in key attestation", e);
        }
    }

}
