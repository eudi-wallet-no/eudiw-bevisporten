package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.Authorization;
import no.idporten.eudiw.oauth2.server.protocol.TokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When processing a token request")
public class TokenRequestProcessingTest {

    private OAuth2AuthorizationServerBase authorizationServer;
    private ClientMetadata client1;
    private SimpleOpenIDConnectCache cache;

    @BeforeEach
    public void setUp() throws Exception {
        client1 = ClientMetadata.builder().clientId("client1").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .client(client1)
                .build();
        authorizationServer = new OAuth2AuthorizationServerBase(serverConfiguration);
        cache = (SimpleOpenIDConnectCache) serverConfiguration.getCache();
    }

    @Test
    @DisplayName("then requests with invalid grant_type is rejected")
    public void testInvalidGrantType() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "refresh_token");
        request.addParameter("code", "c");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertTrue(e.errorDescription().contains("Invalid parameter grant_type"));
    }

    @Test
    @DisplayName("then requests without a grant_type is rejected")
    public void testMissingGrantType() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertTrue(e.errorDescription().contains("Invalid parameter grant_type"));
    }

    @Test
    @DisplayName("then requests without an authorization code is rejected")
    public void testMissingCode() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertTrue(e.errorDescription().contains("Invalid parameter code"));
    }

    @Test
    @DisplayName("then requests with an unknown authorization code is rejected")
    public void testUnknownAuthorizationCode() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        request.addParameter("code_verifier", "RxsIXCOY_4PZdei6pfv6D0T9Dp0Fhfh2GfQR0bU554M");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertAll(
                () -> assertEquals("invalid_grant", e.error()),
                () -> assertTrue(e.errorDescription().contains("does not exist"))
        );
    }

    @Test
    @DisplayName("then requests with an authorization code issued to another client is rejected")
    public void testAuthorizationCodeIssuedToAnotherClient() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        request.addParameter("code_verifier", "Oxu73SxN_YdXaR6D7kp8amib-lrTe0P27qFu-jun99o");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        Authorization authorization = Authorization.builder().aud("client2").build();
        authorization.setLifetimeSeconds(10);
        cache.putAuthorization("c", authorization);
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertAll(
                () -> assertEquals("invalid_grant", e.error()),
                () -> assertTrue(e.errorDescription().contains("is not issued to authenticated client"))
        );
    }

    @Test
    @DisplayName("then requests with an invalid code verifier is rejected")
    public void testInvalidCodeVerfier() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        request.addParameter("code_verifier", "cv");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        Authorization authorization = Authorization.builder().aud("client1").codeChallenge("foobar").build();
        authorization.setLifetimeSeconds(10);
        cache.putAuthorization("c", authorization);
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertAll(
                () -> assertEquals("invalid_request", e.error()),
                () -> assertTrue(e.errorDescription().contains("Invalid parameter code_verifier"))
        );
    }

    @Test
    @DisplayName("then requests with missing code verifier is rejected when pkce is required")
    public void testMissingCodeVerifier() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertAll(
                () -> assertEquals("invalid_request", e.error()),
                () -> assertTrue(e.errorDescription().contains("Missing parameter code_verifier"))
        );
    }

    @Test
    @DisplayName("then requests with too short code verifier is rejected")
    public void testInvalidCodeVerifierTooShort() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        request.addParameter("code_verifier", "kort");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertAll(
                () -> assertEquals("invalid_request", e.error()),
                () -> assertTrue(e.errorDescription().contains("Invalid parameter code_verifier."))
        );
    }

    @Test
    @DisplayName("then requests with invalid code verifier is rejected")
    public void testInvalidCodeVerifierInvalidCharacters() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        request.addParameter("code_verifier", "yXbcA9SFmU5hAeMka1bj_E9B1yV_E-A1QdmKM-k8æææ");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertAll(
                () -> assertEquals("invalid_request", e.error()),
                () -> assertTrue(e.errorDescription().contains("Invalid parameter code_verifier."))
        );
    }

    @Test
    @DisplayName("then requests with invalid DPoP header is rejected")
    public void testInvalidDPoPHeader() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        request.addParameter("code_verifier", "0bIIA7r2upg7LcYGak1kWWJlndTO4FtDW8smeRhjf_I");
        request.addHeader("dPoP", "invalid-dpop-header");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        Authorization authorization = Authorization.builder().aud("client1").codeChallenge("J2dCSyTeiT6L9UtOYYbGY8QptRqlAA7ExQN5xsh-QzI").build();
        authorization.setLifetimeSeconds(100);
        authorization.setClientId(client1.getClientId());
        cache.putAuthorization("c", authorization);
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.process(tokenRequest));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_DPOP_PROOF, e.error()),
                () -> assertTrue(e.errorDescription().contains("Failed to parse DPop header"))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("then a valid token request is accepted")
    public void testValidateTokenRequest(boolean hasDPoP) throws Exception {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", "c");
        request.addParameter("code_verifier", "yXbcA9SFmU5hAeMka1bj_E9B1yV_E-A1QdmKM-k8zw4");
        if (hasDPoP) {
            request.addHeader("dpop", TestUtils.createDpopHeader(URI.create("http://my-unit-test/token")));
        }
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        authorizationServer.validate(tokenRequest, client1);
    }

}
