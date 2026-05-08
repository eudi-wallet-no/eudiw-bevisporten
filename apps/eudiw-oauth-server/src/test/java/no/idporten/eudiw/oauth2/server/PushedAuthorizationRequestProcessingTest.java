package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.*;

import java.net.URI;

import static no.idporten.eudiw.oauth2.server.TestUtils.findDpopJktFromDpopHeader;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When processing a pushed authorization request")
public class PushedAuthorizationRequestProcessingTest {

    public static final String TOKEN_ENDPOINT_URI = "https://junit.idporten.no/token";
    public static final String PAR_ENDPOINT_URI= "https://junit.idporten.no/par";

    private OAuth2AuthorizationServerBase authorizationServer;
    private ClientMetadata client1;
    private SimpleOpenIDConnectCache cache;

    @BeforeEach
    public void setUp() throws Exception {
        client1 = ClientMetadata.builder().clientId("client1").clientSecret("secret").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .client(client1)
                .authorizationDetailsTypeSupported("foo")
                .tokenEndpoint(new URI(TOKEN_ENDPOINT_URI))
                .pushedAuthorizationRequestEndpoint(new URI(PAR_ENDPOINT_URI))
                .build();
        authorizationServer = new OAuth2AuthorizationServerBase(serverConfiguration);
        cache = (SimpleOpenIDConnectCache) serverConfiguration.getCache();
    }

    @Test
    @DisplayName("then the client_id parameter must be present")
    public void testMissingClientId() {
        MockRequest request = new MockRequest();
        try {
            authorizationServer.validateClientId(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Missing parameter client_id"))
            );
        }
    }

    @Test
    @DisplayName("then the client_id parameter must matched the id of the authenticated client")
    public void testMismatchedClientId() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "foo");
        try {
            authorizationServer.validateClientId(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("The request was pushed by another client"))
            );
        }
    }

    @Test
    @DisplayName("then the redirect_uri parameter must be present")
    public void testMissingRedirectUri() {
        MockRequest request = new MockRequest();
        try {
            authorizationServer.validateRedirectUri(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Missing parameter redirect_uri"))
            );
        }
    }

    @Test
    @DisplayName("then the redirect_uri parameter must be registered on client")
    public void testInvalidRedirectUri() {
        MockRequest request = new MockRequest();
        request.addParameter("redirect_uri", "https://junit.digdir.no");
        try {
            authorizationServer.validateRedirectUri(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Not registered on client"))
            );
        }
    }

    @Test
    @DisplayName("then only code flow is supported")
    public void testOnlyCodeFlowSupported() {
        MockRequest request = new MockRequest();
        request.addParameter("response_type", "refresh_token");
        try {
            authorizationServer.validateResponseType(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("unsupported_response_type", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Only authorization code flow is supported"))
            );
        }
    }

    @Test
    @DisplayName("then scopes must be requested")
    public void testNoScopesRequested() {
        MockRequest request = new MockRequest();
        try {
            authorizationServer.validateScope(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_scope", e.error()),
                    () -> assertTrue(e.errorDescription().contains("No scopes requested"))
            );
        }
    }

    @Disabled // TODO
    @Test
    @DisplayName("then openid scope must be requested for the request to be an OpenID Connect request")
    public void testNoOpenidScopeRequested() {
        MockRequest request = new MockRequest();
        request.addParameter("scope", "foo");
        try {
            authorizationServer.validateScope(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_scope", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Not an OpenID Connect request"))
            );
        }
    }

    @Disabled // TODO
    @Test
    @DisplayName("then all requested scopes must be registered with client")
    public void testNotAllScopesReqisteredOnClient() {
        MockRequest request = new MockRequest();
        request.addParameter("scope", "openid foo");
        try {
            authorizationServer.validateScope(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_scope", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Client does not have access to all of the requested scopes"))
            );
        }
    }

    @Disabled // TODO
    @Test
    @DisplayName("then the ui_locales parameter is optional and locale is resolved automatically")
    public void testUiLocalesResolved() {
        MockRequest request = new MockRequest();
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        authorizationServer.validateUiLocales(authorizationRequest, ClientMetadata.builder().build());
        assertEquals("nn", authorizationRequest.getResolvedUiLocale());
    }

    @Disabled // TODO
    @Test
    @DisplayName("then the ui_locales parameter resolves to default for invalid values")
    public void tesInvbalidtUiLocales() {
        MockRequest request = new MockRequest();
        request.addParameter("ui_localeds", "foo bar");
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        authorizationServer.validateUiLocales(authorizationRequest, ClientMetadata.builder().build());
        assertEquals("nn", authorizationRequest.getResolvedUiLocale());
    }

    @Test
    @DisplayName("then the response_mode parameter is optional and default response_mode is query")
    public void testOptionalResponseModeResolved() {
        MockRequest request = new MockRequest();
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        authorizationServer.validateResponseMode(authorizationRequest, ClientMetadata.builder().build());
        assertEquals("query", authorizationRequest.getResolvedResponseMode());
    }

    @Test
    @DisplayName("then the response_mode parameter must contain a valid value")
    public void testInvalidResponseModeResolved() {
        MockRequest request = new MockRequest();
        request.addParameter("response_mode", "foo");
        try {
            authorizationServer.validateResponseMode(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Unsupported response mode"))
            );
        }
    }

    @Test
    @DisplayName("then the state parameter is optional")
    public void testStateIsOptional() {
        MockRequest request = new MockRequest();
        authorizationServer.validateState(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
    }

    @Test
    @DisplayName("then the state parameter must contain legal characters")
    public void testStateInvalidCharacters() {
        MockRequest request = new MockRequest();
        request.addParameter("state", "æøå");
        try {
            authorizationServer.validateState(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid parameter state"))
            );
        }
    }

    @Test
    @DisplayName("then the nonce parameter is optional")
    public void testNonceIsOptional() {
        MockRequest request = new MockRequest();
        authorizationServer.validateNonce(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
    }

    @Test
    @DisplayName("then the nonce parameter must contain legal characters")
    public void testNoneInvalidCharacters() {
        MockRequest request = new MockRequest();
        request.addParameter("nonce", "æøå");
        try {
            authorizationServer.validateNonce(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid parameter nonce"))
            );
        }
    }

    @Test
    @DisplayName("then the authorization_details parameter is optional")
    public void testAuthorizationDetailsIsOptional() {
        MockRequest request = new MockRequest();
        assertDoesNotThrow(() -> authorizationServer.validateAuthorizationDetails(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
    }

    @Test
    @DisplayName("then all authorization_detail in the authorization_details parameter must contain the type attribute")
    public void testAllAuthorizationDetailsMustContainType() {
        MockRequest request = new MockRequest();
        request.addParameter("authorization_details", "[{\"type\": \"foo\"}, {}]");
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateAuthorizationDetails(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        assertAll(
                () -> assertEquals("invalid_authorization_details", e.error()),
                () -> assertTrue(e.errorDescription().contains("type"))
        );
    }

    @Test
    @DisplayName("then a code_challenge_method registered with the server can be used")
    public void testValidCodeChallengeMethod() {
        MockRequest request = new MockRequest();
        request.addParameter("code_challenge_method", "S256");
        request.addParameter("code_challenge", "cc");
        assertDoesNotThrow(() -> authorizationServer.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
    }

    @Test
    @DisplayName("then the resource parameter is optional")
    public void testResourceIsOptional() {
        MockRequest request = new MockRequest();
        assertDoesNotThrow(() -> authorizationServer.validateResource(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
    }

    @Test
    @DisplayName("then the resource parameter must be an absolute uri without query or fragment components")
    public void testResourceValidation() {
        MockRequest request = new MockRequest();
        request.addParameter("resource", "https://api.junit.idporten.dev/v1");
        assertDoesNotThrow(() -> authorizationServer.validateResource(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        request.setParameter("resource", "https://api.junit.idporten.dev/v1#fragment");
        assertThrows(OAuth2Exception.class, () -> authorizationServer.validateResource(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        request.setParameter("resource", "https://api.junit.idporten.dev/v1?query=notallowed");
        assertThrows(OAuth2Exception.class, () -> authorizationServer.validateResource(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        request.setParameter("resource", "v1/api");
        assertThrows(OAuth2Exception.class, () -> authorizationServer.validateResource(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
    }

    @Test
    @DisplayName("then a code_challenge_method not registered with the server cannot be used")
    public void testInvalidCodeChallengeMethod() {
        MockRequest request = new MockRequest();
        request.addParameter("code_challenge_method", "plain");
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        assertAll(
                () -> assertEquals("invalid_request", e.error()),
                () -> assertTrue(e.errorDescription().contains("code_challenge_method"))
        );
    }

    @Test
    @DisplayName("then both code_challenge and code_challenge_method must be used")
    public void testMissingCodeChallenge() {
        MockRequest request = new MockRequest();
        request.addParameter("code_challenge_method", "S256");
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        assertAll(
                () -> assertEquals("invalid_request", e.error()),
                () -> assertTrue(e.errorDescription().contains("Missing parameter code_challenge"))
        );
    }

    @Test
    @DisplayName("then pkce can be configured as optional")
    public void testPKCECanBeOptional() throws Exception {
        client1 = ClientMetadata.builder().clientId("client1").clientSecret("secret").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .client(client1)
                .authorizationDetailsTypeSupported("foo")
                .requirePkce(false)
                .build();
        OAuth2AuthorizationServerBase authorizationServer = new OAuth2AuthorizationServerBase(serverConfiguration);
        cache = (SimpleOpenIDConnectCache) serverConfiguration.getCache();
        MockRequest request = new MockRequest();
        assertDoesNotThrow(() -> authorizationServer.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
    }

    @Nested
    class TestDPoPValidation {

        @Test
        @DisplayName("then DPop header is valid if present")
        public void testDPopHeader() throws Exception {
            MockRequest request = new MockRequest();
            request.addHeader("DPoP", TestUtils.createDpopHeader(authorizationServer.getConfiguration().getPushedAuthorizationRequestEndpoint()));
            request.addParameter("client_id", client1.getClientId());

            assertDoesNotThrow(() -> authorizationServer.validateDPoP(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        }

        @Test
        @DisplayName("then DPop header is invalid will fail")
        public void testInvalidDPopHeader() {
            MockRequest request = new MockRequest();
            request.addHeader("DPoP", "not-valid-jwt");
            request.addParameter("client_id", client1.getClientId());

            OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateDPoP(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_DPOP_PROOF, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Failed to parse DPop header"))
            );
        }

        @Test
        @DisplayName("then dpop_jkt parameter is valid if present")
        public void testDPopJktParam() throws Exception {
            MockRequest request = new MockRequest();
            request.addParameter("dpop_jkt", TestUtils.createDpopJtk());
            request.addParameter("client_id", client1.getClientId());

            assertDoesNotThrow(() -> authorizationServer.validateDPoP(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        }

        @Test
        @DisplayName("then dpop_jkt parameter and dpopHeader is valid if equal")
        public void testDPopJktParamAndDpopHeader() throws Exception {

            MockRequest request = new MockRequest();
            String dpopHeader = TestUtils.createDpopHeader(authorizationServer.getConfiguration().getPushedAuthorizationRequestEndpoint());
            request.addHeader("DPoP", dpopHeader);
            request.addParameter("client_id", client1.getClientId());
            request.addParameter("dpop_jkt", findDpopJktFromDpopHeader(dpopHeader));

            assertDoesNotThrow(() -> authorizationServer.validateDPoP(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        }

        @Test
        @DisplayName("then dpop_jkt parameter and dpopHeader throws exception if different")
        public void whenDifferentDPopJktParamAndDpopHeaderShouldFail() throws Exception {

            MockRequest request = new MockRequest();
            request.addHeader("DPoP", TestUtils.createDpopHeader(authorizationServer.getConfiguration().getPushedAuthorizationRequestEndpoint()));
            request.addParameter("dpop_jkt", TestUtils.createDpopJtk());
            request.addParameter("client_id", client1.getClientId());
            OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateDPoP(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_DPOP_PROOF, e.error()),
                    () -> assertTrue(e.errorDescription().contains("must be equal"))
            );
        }

    }


    @Test
    @DisplayName("then a valid request is accepted")
    public void testValidRequest() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "cid");
        request.addParameter("code_challenge_method", "S256");
        request.addParameter("code_challenge", "foo");
        request.addParameter("redirect_uri", "https://idporten.no/");
        request.addParameter("response_type", "code");
        request.addParameter("scope", "openid");
        request.addParameter("acr_values", "Level3");
        request.addParameter("ui_locales", "en");
        request.addParameter("response_mode", "query");
        request.addParameter("state", "hawaii");
        request.addParameter("nonce", "nonsens");
        request.addParameter("authorization_details", "[{\"type\": \"foo\"}]");
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        authorizationServer.validate(authorizationRequest, clientMetadata);
    }

}
