package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.protocol.*;
import no.idporten.eudiw.oauth2.server.audit.OpenIDConnectAuditLogger;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When testing the pre-authorized code flow with the authorization server")
class PreAuthorizedCodeFlowTest {

    private OpenID4VCIAuthorizationServer openID4VCIAuthorizationServer;
    private SimpleOpenIDConnectCache cache;
    @Mock
    private OpenIDConnectAuditLogger auditLogger;
    @Captor
    private ArgumentCaptor<ClientAuthentication> clientAuthenticationCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .auditLogger(auditLogger)
                .build();
        openID4VCIAuthorizationServer = new OpenID4VCIAuthorizationServer(serverConfiguration);
        cache = (SimpleOpenIDConnectCache) serverConfiguration.getCache();
    }

    @ParameterizedTest
    @ValueSource(strings = {"wallet"})
    @NullAndEmptySource
    @DisplayName("then the server's public methods all work together to implement the protocol (this test tests everything...)")
    void testPreAuthorizedCodeFlow(String clientId) throws Exception {
        // 1. Create pre-authorization
        PreAuthorizationRequest preAuthorizationRequest = new PreAuthorizationRequest();
        preAuthorizationRequest.setAud(openID4VCIAuthorizationServer.getConfiguration().getIssuer().toString());
        preAuthorizationRequest.setSub("12345678901");
        preAuthorizationRequest.setScope(List.of("scp1", "scp2"));
        preAuthorizationRequest.setTxId("tid1");
        preAuthorizationRequest.setAuthorizationLifetimeSeconds(999);
        PreAuthorizationResponse preAuthorizationResponse = openID4VCIAuthorizationServer.process(preAuthorizationRequest, TestUtils.headersWithApiKey(TestUtils.defaultApiKey()));
        assertNotNull(preAuthorizationResponse.getPreAuthorizedCode());
        assertEquals(999, preAuthorizationResponse.getExpiresInSeconds(), 10);
        verify(auditLogger).auditAuthorization(any(Authorization.class));
        final String preAuthorizedCode = preAuthorizationResponse.getPreAuthorizedCode();

        // 2. Process token request w/pre-authorized_code
        MockRequest request = new MockRequest();
        if (clientId != null) {
            request.addParameter("client_id", clientId);
        }
        request.addParameter("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.addParameter("pre-authorized_code", preAuthorizedCode);
        request.addParameter("resource", "https://junit-issuer.idporten.dev");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        TokenResponse tokenResponse = openID4VCIAuthorizationServer.process(tokenRequest);
        assertNotNull(tokenResponse);
        assertNull(tokenResponse.getIdToken());
        verify(auditLogger).auditTokenRequest(tokenRequest);
        verify(auditLogger).auditTokenResponse(tokenResponse);

        // 6. Validate the access_token
        JWKSet jwkSet = openID4VCIAuthorizationServer.getPublicJWKSet();
        SignedJWT accessToken = SignedJWT.parse(tokenResponse.getAccessToken());
        JWSHeader accessTokenHeader = accessToken.getHeader();
        assertEquals("test-kid", accessTokenHeader.getKeyID());
        JWTClaimsSet accessTokenClaimsSet = accessToken.getJWTClaimsSet();
        assertTrue(accessToken.verify(new DefaultJWSVerifierFactory().createJWSVerifier(
                accessTokenHeader,
                jwkSet.getKeyByKeyId(accessTokenHeader.getKeyID()).toECKey().toKeyPair().getPublic())));
        assertEquals(TestUtils.defaultIssuer(), accessTokenClaimsSet.getIssuer());
        assertEquals("https://junit-issuer.idporten.dev", accessTokenClaimsSet.getAudience().getFirst());
        assertEquals("12345678901", accessTokenClaimsSet.getClaim("sub"));
        assertEquals("scp1 scp2", accessTokenClaimsSet.getClaim("scope"));
        assertEquals("tid1", accessTokenClaimsSet.getClaim("tx_id"));

        // 8. Check all id's unique
        assertEquals(2, Set.of(preAuthorizedCode, accessTokenClaimsSet.getJWTID()).size());

        // 9. Check cache empty
        assertTrue(cache.isEmpty());
        verify(auditLogger).auditClientAuthentication(clientAuthenticationCaptor.capture());
        verifyNoMoreInteractions(auditLogger);
        assertEquals("none", clientAuthenticationCaptor.getValue().getTokenEndpointAuthMethod());
    }

}
