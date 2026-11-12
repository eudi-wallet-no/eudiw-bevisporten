package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

/**
 * Wallet instance attestation (WIA) passed on from the authorization server in the access_token.
 */
public record WalletInstanceAttestation(SignedJWT jwt) {

    static final String WIA_CLAIM = "wia";

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
        } catch (ParseException e) {
            throw new InvalidAccessTokenException("Invalid wallet instance attestation in access token", e);
        }
    }

}
