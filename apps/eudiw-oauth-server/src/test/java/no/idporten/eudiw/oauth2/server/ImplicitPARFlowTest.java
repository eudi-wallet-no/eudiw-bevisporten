package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.oauth2.server.audit.OpenIDConnectAuditLogger;
import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.protocol.*;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When testing the authorization code flow with direct PAR response")
@Disabled // TODO ikke nødvending i openid4vci
class ImplicitPARFlowTest {

    private OAuth2AuthorizationServerBase authorizationServer;
    private SimpleOpenIDConnectCache cache;
    private OpenIDConnectAuditLogger auditLogger;


    class ServerExtension extends OAuth2AuthorizationServerBase {
        public ServerExtension(OAuth2ServerConfiguration serverConfiguration) {
            super(serverConfiguration);
        }

        @Override
        protected PushedAuthorizationResponse createResponse(PushedAuthorizationRequest authorizationRequest) {
            Authorization authorization = Authorization.builder()
                    .sub("12345678901")
                    .amr("test-amr,test-amr-2, test-amr-3,  test-amr-4") // test multiple amr values - with and without spaces
                    .acr("LevelX")
                    .attribute("a1", "v1")
                    .attribute("a2", "v2")
                    .attribute("list", (Serializable) List.of("a", "b", "c"))
                    .build();
            return super.createDirectPushedAuthorizationResponse(authorizationRequest, authorization);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        auditLogger = mock(OpenIDConnectAuditLogger.class);
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .responseMode("form_post")
                .userinfoEndpoint(new URI(TestUtils.defaultIssuer() + "userinfo"))
                .auditLogger(auditLogger)
                .build();
        authorizationServer = new ServerExtension(serverConfiguration);
        cache = (SimpleOpenIDConnectCache) serverConfiguration.getCache();
    }


    @Test
    @DisplayName("then the server's public methods all work together to implement the protocol (this test tests everything...)")
    void testImplicitPAR() throws Exception {
        // 1. Process pushed authorization request and generate direct response w/tokens
        MockRequest request = new MockRequest();
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, authorizationServer.getConfiguration().getIssuer().toString()).serialize());
        request.addParameter("code_challenge", "WWHTYIjNclXxS69q1gerQ-eTlW5ab1YCpKTorurQ3zw");
        request.addParameter("code_challenge_method", "S256");
        request.addParameter("scope", "openid");
        request.addParameter("redirect_uri", clientMetadata.getRedirectUris().get(0));
        request.addParameter("response_type", "code");
        request.addParameter("response_mode", "form_post");
        request.addParameter("state", "s");
        request.addParameter("nonce", "n");
        request.addParameter("acr_values", "Level4 Level3");

        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        DirectPushedAuthorizationResponse pushedAuthorizationResponse = (DirectPushedAuthorizationResponse) authorizationServer.process(pushedAuthorizationRequest);
        assertNotNull(pushedAuthorizationResponse);
        assertNull(pushedAuthorizationResponse.getRequestUri());
        assertEquals(200, pushedAuthorizationResponse.getHttpStatusCode());
        assertEquals("s", pushedAuthorizationResponse.getState());
        assertNotNull(pushedAuthorizationResponse.getIdToken());
        assertNotNull(pushedAuthorizationResponse.getAccessToken());
        assertEquals("Bearer", pushedAuthorizationResponse.getTokenType());
        assertTrue(pushedAuthorizationResponse.getExpiresIn() > 0);
        verify(auditLogger).auditPushedAuthorizationRequest(pushedAuthorizationRequest);
        verify(auditLogger).auditAuthorization(any(Authorization.class));
        verify(auditLogger).auditPushedAuthorizationResponse(pushedAuthorizationResponse);

        // 2. Validate the id_token
        JWKSet jwkSet = authorizationServer.getPublicJWKSet();
        SignedJWT jwt = SignedJWT.parse(pushedAuthorizationResponse.getIdToken());
        JWSHeader jwtHeader = jwt.getHeader();
        assertEquals("test-kid", jwtHeader.getKeyID());
        JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        assertTrue(jwt.verify(new DefaultJWSVerifierFactory().createJWSVerifier(
                jwtHeader,
                jwkSet.getKeyByKeyId(jwtHeader.getKeyID()).toRSAKey().toRSAPublicKey())));
        assertEquals(TestUtils.defaultIssuer(), jwtClaimsSet.getIssuer());
        assertEquals(clientMetadata.getClientId(), jwtClaimsSet.getAudience().get(0));
        assertEquals("n", jwtClaimsSet.getClaim("nonce"));
        assertEquals("12345678901", jwtClaimsSet.getClaim("sub"));
        assertEquals("test-amr", jwtClaimsSet.getStringArrayClaim("amr")[0]);
        assertEquals("test-amr-2", jwtClaimsSet.getStringArrayClaim("amr")[1]);
        assertEquals("test-amr-3", jwtClaimsSet.getStringArrayClaim("amr")[2]);
        assertEquals("test-amr-4", jwtClaimsSet.getStringArrayClaim("amr")[3]);
        assertEquals("LevelX", jwtClaimsSet.getClaim("acr"));
        assertEquals("v1", jwtClaimsSet.getClaim("a1"));
        assertEquals("v2", jwtClaimsSet.getClaim("a2"));
        assertTrue(jwtClaimsSet.getStringListClaim("list").contains("a"));
        assertTrue(jwtClaimsSet.getStringListClaim("list").contains("b"));
        assertTrue(jwtClaimsSet.getStringListClaim("list").contains("c"));

        // 3. Check cache empty
        assertTrue(cache.getAuthorizationRequestMap().isEmpty());
        assertTrue(cache.getCode2authorizationMap().isEmpty());
        assertTrue(cache.getAccessToken2authorizationMap().isEmpty());
        verify(auditLogger, times(1)).auditClientAuthentication(any(ClientAuthentication.class));
        verifyNoMoreInteractions(auditLogger);
    }

}
