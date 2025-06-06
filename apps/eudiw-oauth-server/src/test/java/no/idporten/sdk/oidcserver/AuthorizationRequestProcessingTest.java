package no.idporten.sdk.oidcserver;

import no.idporten.sdk.oidcserver.audit.OpenIDConnectAuditLogger;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.AuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When processing an authorization request")
public class AuthorizationRequestProcessingTest {

    private OpenIDConnectIntegrationBase openIDConnectSdk;
    private ClientMetadata client1;
    private SimpleOpenIDConnectCache cache;
    private OpenIDConnectAuditLogger auditLogger;

    @BeforeEach
    public void setUp() throws Exception {
        auditLogger = mock(OpenIDConnectAuditLogger.class);
        client1 = ClientMetadata.builder().clientId("client1").clientSecret("secret").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .auditLogger(auditLogger)
                .client(client1)
                .build();
        openIDConnectSdk = new OpenIDConnectIntegrationBase(sdkConfiguration);
        cache = (SimpleOpenIDConnectCache) sdkConfiguration.getCache();
    }

    @Test
    @DisplayName("then the request_uri parameter is required")
    public void testMissingRequestUri() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "c");
        try {
            openIDConnectSdk.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Missing parameter request_uri"))
            );
        }
        verifyNoInteractions(auditLogger);
    }

    @Test
    @DisplayName("then the client_id parameter is required")
    public void testMissingClientId() {
        MockRequest request = new MockRequest();
        request.addParameter("request_uri", "urn:idporten:abcd");
        try {
            openIDConnectSdk.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertFalse(openIDConnectSdk.getSDKConfiguration().isDisableClientIdCheckOnPARAuthorizationRequests()),
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Missing parameter client_id"))
            );
        }
        verifyNoInteractions(auditLogger);
    }


    @Test
    @DisplayName("then the request_uri parameter must be valid")
    public void testInvalidRequestUri() {
        MockRequest request = new MockRequest();
        request.addParameter("request_uri", "foo");
        try {
            openIDConnectSdk.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid parameter request_uri"))
            );
        }
        verifyNoInteractions(auditLogger);
    }

    @Test
    @DisplayName("then the request_uri parameter must reference a valid pushed authorization request")
    public void testPushedAuthorizationRequestNotValid() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "c");
        request.addParameter("request_uri", "urn:idporten:1234");
        try {
            openIDConnectSdk.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid parameter request_uri")),
                    () -> assertTrue(e.errorDescription().contains("does not exist"))
            );
        }
        verifyNoInteractions(auditLogger);
    }

    @Test
    @DisplayName("then the client_id parameter must match the client_id of the referenced pushed authorization request")
    public void testClientIdInRequestsDoesNotMatch() {
        String requestUri = "urn:idporten:1234";
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "client1");
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        pushedAuthorizationRequest.setLifetimeSeconds(10);
        cache.putAuthorizationRequest(requestUri, pushedAuthorizationRequest);
        request = new MockRequest();
        request.addParameter("client_id", "client2");
        request.addParameter("request_uri", requestUri);
        PushedAuthorizationRequest cachedPushedAuthorizationRequest = null;
        try {
            openIDConnectSdk.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_request", e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid parameter client_id")),
                    () -> assertTrue(e.errorDescription().contains("pushed by another client"))
            );
        }
        verifyNoInteractions(auditLogger);
    }




    @Test
    @DisplayName("then a valid request_uri identifies a pushed authorization request")
    public void testProcessValidRequest() {
        String requestUri = "urn:idporten:1234";
        String clientId = TestUtils.defaultClientMetadata().getClientId();
        MockRequest request = new MockRequest();
        request.addParameter("client_id", clientId);
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        pushedAuthorizationRequest.setLifetimeSeconds(10);
        cache.putAuthorizationRequest(requestUri, pushedAuthorizationRequest);
        request = new MockRequest();
        request.addParameter("client_id", clientId);
        request.addParameter("request_uri", requestUri);
        PushedAuthorizationRequest cachedPushedAuthorizationRequest = openIDConnectSdk.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
        assertAll(
                () -> assertFalse(openIDConnectSdk.getSDKConfiguration().isDisableClientIdCheckOnPARAuthorizationRequests()),
                () -> assertNotNull(cachedPushedAuthorizationRequest),
                () -> assertEquals(clientId, cachedPushedAuthorizationRequest.getClientId()),
                () -> assertNull(cachedPushedAuthorizationRequest.getClientSecret()),
                () -> assertNull(cachedPushedAuthorizationRequest.getAuthorizationHeader()),
                () -> assertTrue(cachedPushedAuthorizationRequest.isValidNow())
        );
        ArgumentCaptor<AuthorizationRequest> authorizationRequestCaptor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        verify(auditLogger).auditAuthorizationRequest(authorizationRequestCaptor.capture());
        assertAll(
                () -> assertEquals(requestUri, authorizationRequestCaptor.getValue().getAuditData().getAttribute("request_uri")),
                () -> assertEquals(clientId, authorizationRequestCaptor.getValue().getAuditData().getAttribute("client_id"))
        );
    }

    @Test
    @DisplayName("then a request without client_id is allowed if client_id check is disabled")
    public void testProcessValidRequestWithoutClientId() throws Exception {
        String requestUri = "urn:idporten:1234";
        String clientId = TestUtils.defaultClientMetadata().getClientId();
        MockRequest request = new MockRequest();
        request.addParameter("client_id", clientId);
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        pushedAuthorizationRequest.setLifetimeSeconds(10);
        SimpleOpenIDConnectCache cache = new SimpleOpenIDConnectCache();
        cache.putAuthorizationRequest(requestUri, pushedAuthorizationRequest);
        request = new MockRequest();
        request.addParameter("request_uri", requestUri);

        OpenIDConnectIntegrationBase localOpenIDConnectSDK = new OpenIDConnectIntegrationBase(
                TestUtils.defaultSdkTestConfigurationBuilder()
                        .disableClientIdCheckOnPARAuthorizationRequests(true)
                        .cache(cache)
                        .auditLogger(auditLogger)
                        .build());

        PushedAuthorizationRequest cachedPushedAuthorizationRequest = localOpenIDConnectSDK.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
        assertAll(
                () -> assertTrue(localOpenIDConnectSDK.getSDKConfiguration().isDisableClientIdCheckOnPARAuthorizationRequests()),
                () -> assertNotNull(cachedPushedAuthorizationRequest),
                () -> assertEquals(clientId, pushedAuthorizationRequest.getClientId())
        );
        ArgumentCaptor<AuthorizationRequest> authorizationRequestCaptor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        verify(auditLogger).auditAuthorizationRequest(authorizationRequestCaptor.capture());
        assertAll(
                () -> assertEquals(requestUri, authorizationRequestCaptor.getValue().getAuditData().getAttribute("request_uri")),
                () -> assertNull(authorizationRequestCaptor.getValue().getAuditData().getAttribute("client_id"))
        );
    }

}
