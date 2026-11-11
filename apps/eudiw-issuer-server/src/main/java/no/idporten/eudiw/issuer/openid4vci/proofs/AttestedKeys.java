package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;

import java.util.List;

/**
 * Binding keys from one validated proof and, if present, the key attestation that attested them.
 *
 * @param keys public binding keys
 * @param keyAttestation validated key attestation, or null if the keys are not attested
 */
public record AttestedKeys(List<JWK> keys, SignedJWT keyAttestation) {

    public AttestedKeys {
        keys = List.copyOf(keys);
    }

    /**
     * @return true if the keys are attested by a key attestation.  This is a temporary workaround.
     */
    public boolean isAttested() {
        return keyAttestation != null;
    }

}
