package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.audit.OpenIDConnectAuditLogger;
import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.AuthorizationRequest;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When processing an authorization request")
public class AuthorizationRequestProcessingTest {

    private OAuth2AuthorizationServerBase oAuth2AuthorizationServer;
    private ClientMetadata client1;
    private SimpleOpenIDConnectCache cache;
    private OpenIDConnectAuditLogger auditLogger;

    @BeforeEach
    public void setUp() throws Exception {
        auditLogger = mock(OpenIDConnectAuditLogger.class);
        client1 = ClientMetadata.builder().clientId("client1").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OAuth2ServerConfiguration oAuth2ServerConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .auditLogger(auditLogger)
                .client(client1)
                .build();
        oAuth2AuthorizationServer = new OAuth2AuthorizationServerBase(oAuth2ServerConfiguration);
        cache = (SimpleOpenIDConnectCache) oAuth2ServerConfiguration.getCache();
    }

    @Test
    @DisplayName("then the request_uri parameter is required")
    public void testMissingRequestUri() {
        MockRequest request = new MockRequest();
        request.addParameter("client_id", "c");
        try {
            oAuth2AuthorizationServer.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
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
            oAuth2AuthorizationServer.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertFalse(oAuth2AuthorizationServer.getConfiguration().isDisableClientIdCheckOnPARAuthorizationRequests()),
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
            oAuth2AuthorizationServer.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
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
            oAuth2AuthorizationServer.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
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
            oAuth2AuthorizationServer.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
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
        PushedAuthorizationRequest cachedPushedAuthorizationRequest = oAuth2AuthorizationServer.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
        assertAll(
                () -> assertFalse(oAuth2AuthorizationServer.getConfiguration().isDisableClientIdCheckOnPARAuthorizationRequests()),
                () -> assertNotNull(cachedPushedAuthorizationRequest),
                () -> assertEquals(clientId, cachedPushedAuthorizationRequest.getClientId()),
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

        OAuth2AuthorizationServerBase localServer = new OAuth2AuthorizationServerBase(
                TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                        .disableClientIdCheckOnPARAuthorizationRequests(true)
                        .cache(cache)
                        .auditLogger(auditLogger)
                        .build());

        PushedAuthorizationRequest cachedPushedAuthorizationRequest = localServer.process(new AuthorizationRequest(request.getHeaders(), request.getParameters()));
        assertAll(
                () -> assertTrue(localServer.getConfiguration().isDisableClientIdCheckOnPARAuthorizationRequests()),
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
