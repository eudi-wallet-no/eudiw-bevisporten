package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When comparing two WIAs")
class WiaProcessingTest {

    private OAuth2AuthorizationServerBase authorizationServer;
    private ECKey walletInstanceKey;

    @BeforeEach
    void setUp() throws Exception {
        authorizationServer = new OAuth2AuthorizationServerBase(TestUtils.defaultOAuth2ServerTestConfigurationBuilder().build());
        walletInstanceKey = new ECKeyGenerator(Curve.P_256).generate();
    }

    private String wia(String clientId, ECKey key) throws Exception {
        return TestUtils.createClientAttestation(clientId, "jw", "https://jw.eidas2sandkasse.dev", key).serialize();
    }

    @DisplayName("then a WIA is equal to itself")
    @Test
    void sameWia() throws Exception {
        String wia = wia("client", walletInstanceKey);
        assertTrue(authorizationServer.wiaEquals(wia, wia));
    }

    @DisplayName("then two different WIAs for the same client and wallet instance key are equal")
    @Test
    void renewedWia() throws Exception {
        String wia1 = wia("client", walletInstanceKey);
        String wia2 = wia("client", walletInstanceKey);
        assertFalse(wia1.equals(wia2), "test precondition: WIAs differ (unique jti)");
        assertTrue(authorizationServer.wiaEquals(wia1, wia2));
    }

    @DisplayName("then WIAs with different wallet instance keys are not equal")
    @Test
    void differentKey() throws Exception {
        ECKey otherKey = new ECKeyGenerator(Curve.P_256).generate();
        assertFalse(authorizationServer.wiaEquals(wia("client", walletInstanceKey), wia("client", otherKey)));
    }

    @DisplayName("then WIAs with different subjects are not equal")
    @Test
    void differentSubject() throws Exception {
        assertFalse(authorizationServer.wiaEquals(wia("client", walletInstanceKey), wia("other-client", walletInstanceKey)));
    }

    @DisplayName("then two missing WIAs are equal and one missing WIA is not")
    @Test
    void nullHandling() throws Exception {
        String wia = wia("client", walletInstanceKey);
        assertTrue(authorizationServer.wiaEquals(null, null));
        assertFalse(authorizationServer.wiaEquals(wia, null));
        assertFalse(authorizationServer.wiaEquals(null, wia));
    }

    @DisplayName("then an unparseable WIA is not equal")
    @Test
    void invalidWia() throws Exception {
        assertFalse(authorizationServer.wiaEquals(wia("client", walletInstanceKey), "not-a-jwt"));
    }

}
