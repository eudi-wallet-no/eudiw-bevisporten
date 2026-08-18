package no.idporten.eudiw.issuer.issuance;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.oauth2.InvalidAccessTokenException;

import java.text.ParseException;

/**
 * Determined from the access token at the start of the issuance process and carried through
 * the {@link no.idporten.eudiw.issuer.claimssource.CredentialIssueContext}
 */
public enum CredentialIssuanceType {

    PRE_AUTHORIZED_CODE,
    AUTHORIZATION_CODE;

    public static CredentialIssuanceType fromAccessToken(JWT accessToken) {
        try {
            return accessToken.getJWTClaimsSet().getStringClaim("tx_id") != null
                    ? PRE_AUTHORIZED_CODE
                    : AUTHORIZATION_CODE;
        } catch (ParseException e) {
            throw new InvalidAccessTokenException("Invalid access token", e);
        }
    }

}
