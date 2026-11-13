package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.credentials.status.StatusList;
import no.idporten.eudiw.issuer.oauth2.InvalidAccessTokenException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Date;
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
    void shouldReadClientStatusFromWia() {
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("wia", walletInstanceAttestationJwt().serialize()).build());

        WalletInstanceAttestation walletInstanceAttestation = WalletInstanceAttestation.fromAccessToken(accessToken);

        assertEquals(
                new AttestationStatus(
                        new StatusList(1337, URI.create("https://junit-wallet-provider.example/wia-statuslists/42")),
                        new Date(4102444800000L)),
                walletInstanceAttestation.clientStatus());
    }

    @Test
    void shouldHaveNoClientStatusWhenWiaHasNone() {
        String wia = walletInstanceAttestationJwt(new JWTClaimsSet.Builder().claim("wallet_name", "junit-wallet").build()).serialize();
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("wia", wia).build());

        assertNull(WalletInstanceAttestation.fromAccessToken(accessToken).clientStatus());
    }

    @Test
    void shouldRejectWiaWithInvalidClientStatus() {
        String wia = walletInstanceAttestationJwt(new JWTClaimsSet.Builder().claim("client_status", "revoked").build()).serialize();
        PlainJWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("wia", wia).build());

        assertThrows(InvalidAccessTokenException.class, () -> WalletInstanceAttestation.fromAccessToken(accessToken));
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
