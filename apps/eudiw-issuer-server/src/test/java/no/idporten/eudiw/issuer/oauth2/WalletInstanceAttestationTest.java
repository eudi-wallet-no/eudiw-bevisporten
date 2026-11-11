package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static no.idporten.eudiw.issuer.TestData.walletInstanceAttestationJwt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletInstanceAttestationTest {

    @Test
    void shouldReturnNullWhenAccessTokenHasNoWiaClaim() {
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().subject("junit").build());

        assertNull(WalletInstanceAttestation.fromAccessToken(accessToken));
    }

    @Test
    void shouldParseWiaClaimFromAccessToken() {
        String wia = walletInstanceAttestationJwt().serialize();
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("wia", wia).build());

        WalletInstanceAttestation walletInstanceAttestation = WalletInstanceAttestation.fromAccessToken(accessToken);

        assertEquals(wia, walletInstanceAttestation.jwt().serialize());
    }

    @Test
    void shouldRejectWiaClaimThatIsNotAJwt() {
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("wia", "not-a-jwt").build());

        assertThrows(InvalidAccessTokenException.class, () -> WalletInstanceAttestation.fromAccessToken(accessToken));
    }

    @Test
    void shouldRejectWiaClaimThatIsNotAString() {
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("wia", Map.of("foo", "bar")).build());

        assertThrows(InvalidAccessTokenException.class, () -> WalletInstanceAttestation.fromAccessToken(accessToken));
    }

}
