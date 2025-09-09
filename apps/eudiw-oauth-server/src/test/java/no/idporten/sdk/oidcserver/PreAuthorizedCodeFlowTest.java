package no.idporten.sdk.oidcserver;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.oauthserver.server.OAuth2AuthorizationServer;
import no.idporten.sdk.oidcserver.audit.OpenIDConnectAuditLogger;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When testing the pre-authorized code flow with the SDK")
class PreAuthorizedCodeFlowTest {

    private OAuth2AuthorizationServer oAuth2AuthorizationServer;
    private SimpleOpenIDConnectCache cache;
    private OpenIDConnectAuditLogger auditLogger;

    @BeforeEach
    public void setUp() throws Exception {
        auditLogger = mock(OpenIDConnectAuditLogger.class);
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .auditLogger(auditLogger)
                .build();
        oAuth2AuthorizationServer = new OAuth2AuthorizationServer(sdkConfiguration);
        cache = (SimpleOpenIDConnectCache) sdkConfiguration.getCache();
    }


    @Test
    @DisplayName("then the SDK's public methods all work together to implement the protocol (this test tests everything...)")
    void testPreAuthorizedCodeFlow() throws Exception {
        // 1. Create pre-authorization
        PreAuthorizationRequest preAuthorizationRequest = new PreAuthorizationRequest();
        preAuthorizationRequest.setAud(oAuth2AuthorizationServer.getSDKConfiguration().getIssuer().toString());
        preAuthorizationRequest.setSub("12345678901");
        preAuthorizationRequest.setScope(List.of("scp1", "scp2"));
        preAuthorizationRequest.setTxId("tid1");
        preAuthorizationRequest.setAuthorizationLifetimeSeconds(999);
        PreAuthorizationResponse preAuthorizationResponse = oAuth2AuthorizationServer.process(preAuthorizationRequest);
        assertNotNull(preAuthorizationResponse.getPreAuthorizedCode());
        assertEquals(999, preAuthorizationResponse.getExpiresInSeconds(), 10);
        verify(auditLogger).auditAuthorization(any(Authorization.class));
        final String preAuthorizedCode = preAuthorizationResponse.getPreAuthorizedCode();

        // 2. Process token request w/pre-authorized_code
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        MockRequest request = new MockRequest();
        request.addParameter("client_id", clientMetadata.getClientId());
        request.addParameter("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.addParameter("pre-authorized_code", preAuthorizedCode);
        request.addParameter("resource", "https://junit-issuer.idporten.dev");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        TokenResponse tokenResponse = oAuth2AuthorizationServer.process(tokenRequest);
        assertNotNull(tokenResponse);
        assertNull(tokenResponse.getIdToken());
        verify(auditLogger).auditTokenRequest(tokenRequest);
        verify(auditLogger).auditTokenResponse(tokenResponse);

        // 6. Validate the access_token
        JWKSet jwkSet = oAuth2AuthorizationServer.getPublicJWKSet();
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
        verify(auditLogger).auditClientAuthentication(any(ClientAuthentication.class));
        verifyNoMoreInteractions(auditLogger);
    }

}
