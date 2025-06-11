package no.idporten.sdk.oidcserver;

import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When processing a pushed authorization request")
public class PushedAuthorizationRequestProcessingTest {

    private OpenIDConnectIntegrationBase openIDConnectSdk;
    private ClientMetadata client1;
    private SimpleOpenIDConnectCache cache;

    @BeforeEach
    public void setUp() throws Exception {
        client1 = ClientMetadata.builder().clientId("client1").clientSecret("secret").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .client(client1)
                .authorizationDetailsTypeSupported("foo")
                .build();
        openIDConnectSdk = new OpenIDConnectIntegrationBase(sdkConfiguration);
        cache = (SimpleOpenIDConnectCache) sdkConfiguration.getCache();
    }

    @Test
    @DisplayName("then the client_id parameter must be present")
    public void testMissingClientId() {
        MockRequest request = new MockRequest();
        try {
            openIDConnectSdk.validateClientId(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
            openIDConnectSdk.validateClientId(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
            openIDConnectSdk.validateRedirectUri(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
            openIDConnectSdk.validateRedirectUri(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
            openIDConnectSdk.validateResponseType(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
            openIDConnectSdk.validateScope(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
            openIDConnectSdk.validateScope(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
            openIDConnectSdk.validateScope(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
    @DisplayName("then the acr_values parameter is optional and acr is resolved automatically")
    public void testAcrValuesResolved() {
        MockRequest request = new MockRequest();
        request.addParameter("acr_values", "");
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        openIDConnectSdk.validateAcrValues(authorizationRequest, ClientMetadata.builder().build());
        assertEquals("Level3", authorizationRequest.getResolvedAcrValue());
    }

    @Disabled // TODO
    @Test
    @DisplayName("then the acr_values parameter must contain valid values if used")
    public void testAcrValues() {
        MockRequest request = new MockRequest();
        request.addParameter("acr_values", "foo");
        try {
            openIDConnectSdk.validateAcrValues(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Requested values not supported"))
            );
        }
    }

    @Disabled // TODO
    @Test
    @DisplayName("then the ui_locales parameter is optional and locale is resolved automatically")
    public void testUiLocalesResolved() {
        MockRequest request = new MockRequest();
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        openIDConnectSdk.validateUiLocales(authorizationRequest, ClientMetadata.builder().build());
        assertEquals("nn", authorizationRequest.getResolvedUiLocale());
    }

    @Disabled // TODO
    @Test
    @DisplayName("then the ui_locales parameter resolves to default for invalid values")
    public void tesInvbalidtUiLocales() {
        MockRequest request = new MockRequest();
        request.addParameter("ui_localeds", "foo bar");
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        openIDConnectSdk.validateUiLocales(authorizationRequest, ClientMetadata.builder().build());
        assertEquals("nn", authorizationRequest.getResolvedUiLocale());
    }

    @Test
    @DisplayName("then the response_mode parameter is optional and default response_mode is query")
    public void testOptionalResponseModeResolved() {
        MockRequest request = new MockRequest();
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        openIDConnectSdk.validateResponseMode(authorizationRequest, ClientMetadata.builder().build());
        assertEquals("query", authorizationRequest.getResolvedResponseMode());
    }

    @Test
    @DisplayName("then the response_mode parameter must contain a valid value")
    public void testInvalidResponseModeResolved() {
        MockRequest request = new MockRequest();
        request.addParameter("response_mode", "foo");
        try {
            openIDConnectSdk.validateResponseMode(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
        openIDConnectSdk.validateState(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
    }

    @Test
    @DisplayName("then the state parameter must contain legal characters")
    public void testStateInvalidCharacters() {
        MockRequest request = new MockRequest();
        request.addParameter("state", "æøå");
        try {
            openIDConnectSdk.validateState(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
        openIDConnectSdk.validateNonce(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
    }

    @Test
    @DisplayName("then the nonce parameter must contain legal characters")
    public void testNoneInvalidCharacters() {
        MockRequest request = new MockRequest();
        request.addParameter("nonce", "æøå");
        try {
            openIDConnectSdk.validateNonce(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build());
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
        assertDoesNotThrow(() -> openIDConnectSdk.validateAuthorizationDetails(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
    }

    @Test
    @DisplayName("then all authorization_detail in the authorization_details parameter must contain the type attribute")
    public void testAllAuthorizationDetailsMustContainType() {
        MockRequest request = new MockRequest();
        request.addParameter("authorization_details", "[{\"type\": \"foo\"}, {}]");
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> openIDConnectSdk.validateAuthorizationDetails(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
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
        assertDoesNotThrow(() -> openIDConnectSdk.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
    }


    @Test
    @DisplayName("then a code_challenge_method not registered with the server cannot be used")
    public void testInvalidCodeChallengeMethod() {
        MockRequest request = new MockRequest();
        request.addParameter("code_challenge_method", "plain");
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> openIDConnectSdk.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
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
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> openIDConnectSdk.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
        assertAll(
                () -> assertEquals("invalid_request", e.error()),
                () -> assertTrue(e.errorDescription().contains("Missing parameter code_challenge"))
        );
    }

    @Test
    @DisplayName("then pkce can be configured as optional")
    public void testPKCECanBeOptional() throws Exception {
        client1 = ClientMetadata.builder().clientId("client1").clientSecret("secret").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .client(client1)
                .authorizationDetailsTypeSupported("foo")
                .requirePkce(false)
                .build();
        OpenIDConnectIntegrationBase openIDConnectSdk = new OpenIDConnectIntegrationBase(sdkConfiguration);
        cache = (SimpleOpenIDConnectCache) sdkConfiguration.getCache();
        MockRequest request = new MockRequest();
        assertDoesNotThrow(() -> openIDConnectSdk.validateCodeChallenge(new PushedAuthorizationRequest(request.getHeaders(), request.getParameters()), ClientMetadata.builder().build()));
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
        openIDConnectSdk.validate(authorizationRequest, clientMetadata);
    }

}
