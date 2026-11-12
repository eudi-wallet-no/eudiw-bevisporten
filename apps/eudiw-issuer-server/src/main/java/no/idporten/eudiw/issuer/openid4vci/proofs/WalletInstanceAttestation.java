package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.jwt.InvalidClaimException;
import no.idporten.eudiw.issuer.oauth2.InvalidAccessTokenException;

import java.text.ParseException;

/**
 * Wallet instance attestation (WIA) passed on from the authorization server in the access_token.
 *
 * @param jwt the wallet instance attestation
 * @param clientStatus status of the wallet instance from the client_status claim, or null if not present
 */
public record WalletInstanceAttestation(SignedJWT jwt, AttestationStatus clientStatus) {

    static final String WIA_CLAIM = "wia";
    static final String CLIENT_STATUS_CLAIM = "client_status";

    /**
     * Creates wallet instance attestation with client status read from the jwt.
     *
     * @throws InvalidClaimException if the claims or the client_status claim of the jwt are invalid
     */
    public WalletInstanceAttestation(SignedJWT jwt) {
        this(jwt, readClientStatus(jwt));
    }

    /**
     * Extract wallet instance attestation from the wia claim in the access token.
     *
     * @param accessToken validated access token
     * @return wallet instance attestation, or null if the access token has no wia claim
     */
    public static WalletInstanceAttestation fromAccessToken(JWT accessToken) {
        try {
            String wia = accessToken.getJWTClaimsSet().getStringClaim(WIA_CLAIM);
            return wia == null ? null : new WalletInstanceAttestation(SignedJWT.parse(wia));
        } catch (ParseException | InvalidClaimException e) {
            throw new InvalidAccessTokenException("Invalid wallet instance attestation in access token", e);
        }
    }

    private static AttestationStatus readClientStatus(SignedJWT jwt) {
        try {
            return AttestationStatus.fromClaims(jwt.getJWTClaimsSet(), CLIENT_STATUS_CLAIM);
        } catch (ParseException e) {
            throw new InvalidClaimException("Invalid claims in wallet instance attestation", e);
        }
    }

}
