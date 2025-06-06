package no.idporten.sdk.oidcserver;

import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.AuthenticatedRequest;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.TokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static no.idporten.sdk.oidcserver.TestUtils.basicAuthHeader;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When authenticating a client")
public class ClientAuthenticationTest {

    private OpenIDConnectIntegrationBase openIDConnectSdk;

    @BeforeEach
    public void setUp() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .client(ClientMetadata.builder().clientId("anotherclient").clientSecret("secret2").scope("openid").redirectUri("https://junit.idporten.no/").build())
                .build();
        openIDConnectSdk = new OpenIDConnectIntegrationBase(sdkConfiguration);
    }

    @Test
    @DisplayName("then missing client authentication raises an OAuth2 error with code invalid_client")
    public void testMissingClientAuthentication() {
        MockRequest request = new MockRequest();
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        try {
            openIDConnectSdk.authenticateClient(authenticatedRequest);
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Missing client authentication"))
            );
        }
    }

    @Test
    @DisplayName("then multiple client authentications raises an OAuth2 error with code invalid_client")
    public void testMultipleClientAuthentication() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId());
        request.addParameter("client_secret", TestUtils.defaultClientMetadata().getClientSecret());
        request.addHeader("Authorization", basicAuthHeader(TestUtils.defaultClientMetadata().getClientId(), TestUtils.defaultClientMetadata().getClientSecret()));
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        try {
            openIDConnectSdk.authenticateClient(authenticatedRequest);
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Multiple client authentications"))
            );
        }
    }

    @Test
    @DisplayName("then client authentication in header with a different client_id as parameter raises an OAuth2 error with code invalid_client and response code unauthorized")
    public void testMixedClientAuthentications() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId() + "x");
        request.addHeader("Authorization", basicAuthHeader(TestUtils.defaultClientMetadata().getClientId(), TestUtils.defaultClientMetadata().getClientSecret()));
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        try {
            openIDConnectSdk.process(pushedAuthorizationRequest);
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                    () -> assertTrue(e.errorDescription().contains("does not match parameter client_id")),
                    () -> assertEquals(401, e.getHttpStatusCode())
            );
        }
    }

    @Test
    @DisplayName("then authentication method client_secret_post can be used")
    public void testClientSecretPost() {
        MockRequest request = new MockRequest();
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        request.addParameter("client_id", clientMetadata.getClientId());
        request.addParameter("client_secret", clientMetadata.getClientSecret());
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        ClientMetadata authenticatedClient = openIDConnectSdk.authenticateClient(authenticatedRequest);
        assertAll(
                () -> assertEquals(TestUtils.defaultClientMetadata().getClientId(), authenticatedClient.getClientId()),
                () -> assertNull(authenticatedRequest.getClientSecret())
        );
    }

    @Test
    @DisplayName("then authentication method client_secret_basic can be used")
    public void testClientSecretBasic() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId());
        request.addHeader("Authorization", basicAuthHeader(TestUtils.defaultClientMetadata().getClientId(), TestUtils.defaultClientMetadata().getClientSecret()));
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        ClientMetadata clientMetadata = openIDConnectSdk.authenticateClient(authenticatedRequest);
        assertAll(
                () -> assertEquals(TestUtils.defaultClientMetadata().getClientId(), clientMetadata.getClientId()),
                () -> assertNull(authenticatedRequest.getAuthorizationHeader())
        );
    }

    @Test
    @DisplayName("then an invalid authorization header raises an OAuth2 error with code invalid_client and response code unauthorized")
    public void testInvalidAuthorizationHeader() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId());
        request.addHeader("Authorization", "Basic æææ");
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        try {
            openIDConnectSdk.authenticateClient(authenticatedRequest);
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                    () -> assertTrue(e.errorDescription().contains("Invalid Authorization header")),
                    () -> assertEquals(401, e.getHttpStatusCode())
            );
        }
    }

    @Test
    @DisplayName("then an invalid client_secret raises an OAuth2 error with code invalid_client and response code unauthorized")
    public void testInvalidClientSecret() {
        try {
            openIDConnectSdk.authenticateClient(TestUtils.defaultClientMetadata().getClientId(), "x");
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                    () -> assertEquals(401, e.getHttpStatusCode())
            );
        }
    }

    @Test
    @DisplayName("then authentication method client_secret_jwt can be used")
    public void testClientSecretJwt() throws Exception {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        MockRequest request = new MockRequest();
        request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, openIDConnectSdk.getSDKConfiguration().getIssuer().toString()).serialize());
        request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        ClientMetadata authenticatedClientMetadata = openIDConnectSdk.authenticateClient(authenticatedRequest);
        assertAll(
                () -> assertEquals(TestUtils.defaultClientMetadata().getClientId(), authenticatedClientMetadata.getClientId()),
                () -> assertNull(authenticatedRequest.getClientSecret())
        );
    }

    @Test
    @DisplayName("then a jwt with invalid audience is rejected")
    public void testClientSecretJwtInvalidAudience() throws Exception {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        MockRequest request = new MockRequest();
        request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, "foo-issuer").serialize());
        request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> openIDConnectSdk.authenticateClient(authenticatedRequest));
        assertAll(
                () -> assertEquals("invalid_client", exception.error()),
                () -> assertTrue(exception.errorDescription().contains("Unknown JWT audience")),
                () -> assertEquals(401, exception.getHttpStatusCode())
        );
    }

    @Test
    @DisplayName("then a jwt with no audience is rejected")
    public void testClientSecretJwtNoAudience() throws Exception {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        MockRequest request = new MockRequest();
        request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata).serialize());
        request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> openIDConnectSdk.authenticateClient(authenticatedRequest));
        assertAll(
                () -> assertEquals("invalid_client", exception.error()),
                () -> assertTrue(exception.errorDescription().contains("Missing JWT audience")),
                () -> assertEquals(401, exception.getHttpStatusCode())
        );
    }

    @Test
    @DisplayName("then a jwt with multiple audiences is rejected")
    public void testClientSecretJwtMultipleAudience() throws Exception {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        MockRequest request = new MockRequest();
        request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, "aud1", "aud2").serialize());
        request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> openIDConnectSdk.authenticateClient(authenticatedRequest));
        assertAll(
                () -> assertEquals("invalid_client", exception.error()),
                () -> assertTrue(exception.errorDescription().contains("Unique JWT audience required")),
                () -> assertEquals(401, exception.getHttpStatusCode())
        );
    }

    @Test
    @DisplayName("then an unknown client_id raises an OAuth2 error with code invalid_client and response code unauthorized")
    public void testUnknownClient() {
        try {
            openIDConnectSdk.authenticateClient("unknown", "secret");
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                    () -> assertTrue(e.errorDescription().contains("Unknown client")),
                    () -> assertEquals(401, e.getHttpStatusCode())
            );
        }
    }

}
