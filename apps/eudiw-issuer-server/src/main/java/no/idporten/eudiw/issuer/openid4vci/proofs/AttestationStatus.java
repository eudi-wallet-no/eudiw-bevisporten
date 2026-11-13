package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jwt.JWTClaimsSet;
import no.idporten.eudiw.issuer.credentials.status.StatusList;
import no.idporten.eudiw.issuer.jwt.InvalidClaimException;
import no.idporten.eudiw.issuer.jwt.JwtClaims;

import java.net.URI;
import java.util.Date;

/**
 * Status of an attestation.
 *
 * @param statusList status list reference
 * @param exp revocation maintenance expiration time
 */
public record AttestationStatus(StatusList statusList, Date exp) {

    /**
     * Reads attestation status from a claim with status and exp.
     */
    public static AttestationStatus fromClaims(JWTClaimsSet claims, String claimName) {
        if (JwtClaims.getJSONObjectClaim(claims, claimName).isEmpty()) {
            return null;
        }
        return new AttestationStatus(
                statusList(claims, claimName),
                JwtClaims.getDateClaim(claims, claimName, "exp").orElse(null));
    }

    private static StatusList statusList(JWTClaimsSet claims, String claimName) {
        if (JwtClaims.getJSONObjectClaim(claims, claimName, "status", "status_list").isEmpty()) {
            return null;
        }
        int index = JwtClaims.getIntegerClaim(claims, claimName, "status", "status_list", "idx")
                .orElseThrow(() -> new InvalidClaimException("Claim %s.status.status_list.idx is missing".formatted(claimName)));
        URI uri = JwtClaims.getURIClaim(claims, claimName, "status", "status_list", "uri")
                .orElseThrow(() -> new InvalidClaimException("Claim %s.status.status_list.uri is missing".formatted(claimName)));
        return new StatusList(index, uri);
    }

}
