package no.idporten.sdk.oidcserver;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.sdk.oidcserver.audit.OpenIDConnectAuditLogger;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When testing the authorization code flow with the SDK")
class CodeFlowTest {

    private OpenIDConnectIntegrationBase openIDConnectSdk;
    private SimpleOpenIDConnectCache cache;
    private OpenIDConnectAuditLogger auditLogger;

    @BeforeEach
    public void setUp() throws Exception {
        auditLogger = mock(OpenIDConnectAuditLogger.class);
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .responseMode("form_post")
                .userinfoEndpoint(new URI(TestUtils.defaultIssuer() + "userinfo"))
                .auditLogger(auditLogger)
                .build();
        openIDConnectSdk = new OpenIDConnectIntegrationBase(sdkConfiguration);
        cache = (SimpleOpenIDConnectCache) sdkConfiguration.getCache();
    }


    @Test
    @DisplayName("then the SDK's public methods all work together to implement the protocol (this test tests everything...)")
    void testCodeFlow() throws Exception {
        // 1. Process pushed authorization request
        MockRequest request = new MockRequest();
        ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, openIDConnectSdk.getSDKConfiguration().getIssuer().toString()).serialize());
        request.addParameter("code_challenge", "WWHTYIjNclXxS69q1gerQ-eTlW5ab1YCpKTorurQ3zw");
        request.addParameter("code_challenge_method", "S256");
        request.addParameter("scope", "openid pid.mdoc");
        request.addParameter("redirect_uri", clientMetadata.getRedirectUris().get(0));
        request.addParameter("response_type", "code");
        request.addParameter("response_mode", "form_post");
        request.addParameter("state", "s");
        request.addParameter("nonce", "n");
        request.addParameter("acr_values", "Level4 Level3");
        request.addParameter("resource", "https://api.idporten.junit/v1");

        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        PushedAuthorizationResponse pushedAuthorizationResponse = openIDConnectSdk.process(pushedAuthorizationRequest);
        assertNotNull(pushedAuthorizationResponse);
        assertNotNull(pushedAuthorizationResponse.getRequestUri());
        // TODO
//        assertEquals("Level4", pushedAuthorizationRequest.getResolvedAcrValue());
//        assertEquals("nn", pushedAuthorizationRequest.getResolvedUiLocale());
//        assertEquals("form_post", pushedAuthorizationRequest.getResolvedResponseMode());
        assertTrue(pushedAuthorizationResponse.getExpiresIn() > 0);
        verify(auditLogger).auditPushedAuthorizationRequest(pushedAuthorizationRequest);
        verify(auditLogger).auditPushedAuthorizationResponse(pushedAuthorizationResponse);
        final String requestUri = pushedAuthorizationResponse.getRequestUri();

        // 2. Process authorization request
        request = new MockRequest();
        request.addParameter("client_id", clientMetadata.getClientId());
        request.addParameter("request_uri", requestUri);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(request.getHeaders(), request.getParameters());
        PushedAuthorizationRequest cachedRequest = openIDConnectSdk.process(authorizationRequest);
        assertEquals(cachedRequest, pushedAuthorizationRequest);
        verify(auditLogger).auditAuthorizationRequest(authorizationRequest);

        // 3. Create an authorization
        Authorization authorization = Authorization.builder()
                .sub("12345678901")
                .amr("test")
                .acr("LevelX")
                .attribute("a1", "v1")
                .attribute("a2", "v2")
                .attribute("list", (Serializable) List.of("a", "b", "c"))
                .build();
        AuthorizationResponse authorizationResponse = openIDConnectSdk.authorize(cachedRequest, authorization);
        assertNotNull(authorizationResponse);
        assertNotNull(authorizationResponse.getCode());
        assertEquals(clientMetadata.getRedirectUris().get(0), authorizationResponse.getRedirectUri());
        assertEquals("form_post", authorizationResponse.getResponseMode());
        assertEquals("s", authorizationResponse.getState());
        assertEquals(TestUtils.defaultIssuer(), authorizationResponse.getIss());
        assertEquals("LevelX", authorization.getAcr());
        verify(auditLogger).auditAuthorization(authorization);
        verify(auditLogger).auditAuthorizationResponse(authorizationResponse);
        final String code = authorizationResponse.getCode();

        // 4. Process token request
        request = new MockRequest();
        request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, openIDConnectSdk.getSDKConfiguration().getIssuer().toString()).serialize());
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", code);
        request.addParameter("redirect_uri", clientMetadata.getRedirectUris().get(0));
        request.addParameter("code_verifier", "1234567890123456789012345678901234567890123");
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        TokenResponse tokenResponse = openIDConnectSdk.process(tokenRequest);
        assertNotNull(tokenResponse);
        assertNotNull(tokenResponse.getIdToken());
        verify(auditLogger).auditTokenRequest(tokenRequest);
        verify(auditLogger).auditTokenResponse(tokenResponse);

        // 5. Validate the id_token
        JWKSet jwkSet = openIDConnectSdk.getPublicJWKSet();
        SignedJWT idToken = SignedJWT.parse(tokenResponse.getIdToken());
        JWSHeader idTokenHeader = idToken.getHeader();
        assertEquals("test-kid", idTokenHeader.getKeyID());
        JWTClaimsSet idTokenClaimsSet = idToken.getJWTClaimsSet();
        assertTrue(idToken.verify(new DefaultJWSVerifierFactory().createJWSVerifier(
                idTokenHeader,
                jwkSet.getKeyByKeyId(idTokenHeader.getKeyID()).toRSAKey().toRSAPublicKey())));
        assertEquals(TestUtils.defaultIssuer(), idTokenClaimsSet.getIssuer());
        assertEquals(clientMetadata.getClientId(), idTokenClaimsSet.getAudience().getFirst());
        assertEquals("n", idTokenClaimsSet.getClaim("nonce"));
        assertEquals("12345678901", idTokenClaimsSet.getClaim("sub"));
        assertEquals("test", idTokenClaimsSet.getStringArrayClaim("amr")[0]);
        assertEquals("LevelX", idTokenClaimsSet.getClaim("acr"));
        assertEquals("v1", idTokenClaimsSet.getClaim("a1"));
        assertEquals("v2", idTokenClaimsSet.getClaim("a2"));
        assertTrue(idTokenClaimsSet.getStringListClaim("list").contains("a"));
        assertTrue(idTokenClaimsSet.getStringListClaim("list").contains("b"));
        assertTrue(idTokenClaimsSet.getStringListClaim("list").contains("c"));

        // 6. Validate the access_token
        SignedJWT accessToken = SignedJWT.parse(tokenResponse.getAccessToken());
        JWSHeader accessTokenHeader = accessToken.getHeader();
        assertEquals("test-kid", accessTokenHeader.getKeyID());
        JWTClaimsSet accessTokenClaimsSet = accessToken.getJWTClaimsSet();
        assertTrue(accessToken.verify(new DefaultJWSVerifierFactory().createJWSVerifier(
                accessTokenHeader,
                jwkSet.getKeyByKeyId(accessTokenHeader.getKeyID()).toRSAKey().toRSAPublicKey())));
        assertEquals(TestUtils.defaultIssuer(), accessTokenClaimsSet.getIssuer());
        assertEquals("https://api.idporten.junit/v1", accessTokenClaimsSet.getAudience().getFirst());
        assertEquals(clientMetadata.getClientId(), accessTokenClaimsSet.getClaim("client_id"));
        assertEquals("12345678901", accessTokenClaimsSet.getClaim("sub"));

        // 7. Process optional userinfo request
        request = new MockRequest();
        request.addHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());
        UserInfoRequest userInfoRequest = new UserInfoRequest(request.getHeaders(), request.getParameters());
        UserInfoResponse userInfoResponse = openIDConnectSdk.process(userInfoRequest);
        assertEquals("12345678901", userInfoResponse.getSub());
        verify(auditLogger).auditUserInfoRequest(userInfoRequest);
        verify(auditLogger).auditUserInfoResponse(userInfoResponse);

        // 8. Check all id's unique
        assertEquals(4, new HashSet(List.of(requestUri.split(":")[2], code, idTokenClaimsSet.getJWTID(), accessTokenClaimsSet.getJWTID())).size());

        // 9. Check cache empty
        assertTrue(cache.getAuthorizationRequestMap().isEmpty());
        assertTrue(cache.getCode2authorizationMap().isEmpty());
        assertFalse(cache.getAccessToken2authorizationMap().isEmpty());
        verify(auditLogger, times(2)).auditClientAuthentication(any(ClientAuthentication.class));
        verifyNoMoreInteractions(auditLogger);
    }

}
