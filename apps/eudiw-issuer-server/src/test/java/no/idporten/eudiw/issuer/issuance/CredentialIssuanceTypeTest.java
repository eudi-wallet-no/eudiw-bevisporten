package no.idporten.eudiw.issuer.issuance;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.oauth2.InvalidAccessTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("When resolving credential issuance type")
class CredentialIssuanceTypeTest {

    @DisplayName("then tx_id claim maps to pre-authorized code flow")
    @Test
    void preAuthorizedCodeFlow() throws Exception {
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder()
                .claim("tx_id", "tx-123")
                .build());

        assertEquals(CredentialIssuanceType.PRE_AUTHORIZED_CODE, CredentialIssuanceType.fromAccessToken(accessToken));
    }

    @DisplayName("then missing tx_id claim maps to authorization code flow")
    @Test
    void authorizationCodeFlow() throws Exception {
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder()
                .subject("12345678901")
                .build());

        assertEquals(CredentialIssuanceType.AUTHORIZATION_CODE, CredentialIssuanceType.fromAccessToken(accessToken));
    }

    @DisplayName("then non-string tx_id claim is rejected as invalid access token")
    @Test
    void invalidTxIdClaimType() {
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder()
                .claim("tx_id", 123)
                .build());

        assertThrows(InvalidAccessTokenException.class, () -> CredentialIssuanceType.fromAccessToken(accessToken));
    }

}
