package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import no.idporten.eudiw.oauth2.server.audit.OpenIDConnectAuditLogger;
import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.protocol.*;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

import static no.idporten.eudiw.oauth2.server.TestUtils.findDpopJktFromDpopHeader;
import static no.idporten.eudiw.oauth2.server.TestUtils.getDPoPProofFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When testing the authorization code flow with the embedded oauth2 authorization server")
class AuthorizationCodeFlowTest {

    private OAuth2AuthorizationServerBase oAuth2AuthorizationServer;
    private SimpleOpenIDConnectCache cache;
    private DPoPProofFactory proofFactory;
    @Mock
    private OpenIDConnectAuditLogger auditLogger;
    @Captor
    private ArgumentCaptor<ClientAuthentication> clientAuthenticationCaptor;

    public enum DPopTestCase {
        NONE, // no endpoint uses DPoP
        DPOP_ALL, // all endpoints use DPoP (PAR, token)
        DPOP_TOKEN; // only token endpoint uses DPoP
    }

    @BeforeEach
    public void setUp() throws Exception {
        OAuth2ServerConfiguration oAuth2ServerConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .responseMode("form_post")
                .userinfoEndpoint(new URI(TestUtils.defaultIssuer() + "userinfo"))
                .tokenEndpoint(new URI(TestUtils.defaultIssuer() + "token"))
                .pushedAuthorizationRequestEndpoint(new URI(TestUtils.defaultIssuer() + "par"))
                .auditLogger(auditLogger)
                .build();
        oAuth2AuthorizationServer = new OAuth2AuthorizationServerBase(oAuth2ServerConfiguration);
        cache = (SimpleOpenIDConnectCache) oAuth2ServerConfiguration.getCache();
        proofFactory = getDPoPProofFactory();
    }


    @ParameterizedTest
    @EnumSource(DPopTestCase.class)
    @DisplayName("then the server's public methods all work together to implement the protocol (this test tests everything...)")
    void testCodeFlow(DPopTestCase hasDPoP) throws Exception {
        final String dPoPHeader = hasDPoP == DPopTestCase.DPOP_ALL ? TestUtils.createDpopHeader(oAuth2AuthorizationServer.getConfiguration().getPushedAuthorizationRequestEndpoint(), proofFactory) : null;
        final ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
        ECKey clientKey = TestUtils.createECPrivateKey();

        // 0. use attestation based client authentication
        MockRequest request = new MockRequest();
        ChallengeRequest challengeRequest = new ChallengeRequest(request.getHeaders());
        ChallengeResponse challengeResponse = oAuth2AuthorizationServer.process(challengeRequest);
        assertEquals(challengeResponse.getAttestationChallenge(), cache.getChallenge(challengeResponse.getAttestationChallenge()).challenge());
        SignedJWT clientAttestation = TestUtils.createClientAttestation(clientMetadata.getClientId(), clientKey);
        SignedJWT clientAttestationPoP = TestUtils.createClientAttestationPoP(clientMetadata.getClientId(), challengeResponse.getAttestationChallenge(), oAuth2AuthorizationServer.getConfiguration().getIssuer().toString(), clientKey);

        // 1. Process pushed authorization request
        request = new MockRequest();
        request.addHeader("OAuth-Client-Attestation", clientAttestation.serialize());
        request.addHeader("OAuth-Client-Attestation-PoP", clientAttestationPoP.serialize());
        request.addParameter("client_id", clientMetadata.getClientId());
        request.addParameter("code_challenge", "WWHTYIjNclXxS69q1gerQ-eTlW5ab1YCpKTorurQ3zw");
        request.addParameter("code_challenge_method", "S256");
        request.addParameter("scope", "openid pid.mdoc");
        request.addParameter("redirect_uri", clientMetadata.getRedirectUris().getFirst());
        request.addParameter("response_type", "code");
        request.addParameter("response_mode", "form_post");
        request.addParameter("state", "s");
        request.addParameter("nonce", "n");
        request.addParameter("acr_values", "Level4 Level3");
        request.addParameter("resource", "https://api.idporten.junit/v1");
        request.addParameter("issuer_state", "is");
        if (hasDPoP == DPopTestCase.DPOP_ALL) {
            request.addHeader("dpop", dPoPHeader);
        }
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        PushedAuthorizationResponse pushedAuthorizationResponse = oAuth2AuthorizationServer.process(pushedAuthorizationRequest);
        assertNotNull(pushedAuthorizationResponse);
        assertNotNull(pushedAuthorizationResponse.getRequestUri());
        assertEquals(201, pushedAuthorizationResponse.getHttpStatusCode());
        // TODO
//        assertEquals("Level4", pushedAuthorizationRequest.getResolvedAcrValue());
//        assertEquals("nn", pushedAuthorizationRequest.getResolvedUiLocale());
//        assertEquals("form_post", pushedAuthorizationRequest.getResolvedResponseMode());
        assertTrue(pushedAuthorizationResponse.getExpiresIn() > 0);
        verify(auditLogger).auditPushedAuthorizationRequest(pushedAuthorizationRequest);
        verify(auditLogger).auditPushedAuthorizationResponse(pushedAuthorizationResponse);
        assertNull(cache.getChallenge(challengeResponse.getAttestationChallenge()));

        final String requestUri = pushedAuthorizationResponse.getRequestUri();

        // 2. Process authorization request
        request = new MockRequest();
        request.addParameter("client_id", clientMetadata.getClientId());
        request.addParameter("request_uri", requestUri);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(request.getHeaders(), request.getParameters());
        PushedAuthorizationRequest cachedRequest = oAuth2AuthorizationServer.process(authorizationRequest);
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
        AuthorizationResponse authorizationResponse = oAuth2AuthorizationServer.authorize(cachedRequest, authorization);
        assertNotNull(authorizationResponse);
        assertNotNull(authorizationResponse.getCode());
        assertEquals(clientMetadata.getRedirectUris().getFirst(), authorizationResponse.getRedirectUri());
        assertEquals("form_post", authorizationResponse.getResponseMode());
        assertEquals("s", authorizationResponse.getState());
        assertEquals(TestUtils.defaultIssuer(), authorizationResponse.getIss());
        assertEquals("LevelX", authorization.getAcr());
        verify(auditLogger).auditAuthorization(authorization);
        verify(auditLogger).auditAuthorizationResponse(authorizationResponse);
        final String code = authorizationResponse.getCode();

        // 4. Process token request
        final String dPoPHeaderToken = hasDPoP == DPopTestCase.DPOP_ALL || hasDPoP == DPopTestCase.DPOP_TOKEN ? TestUtils.createDpopHeader(oAuth2AuthorizationServer.getConfiguration().getTokenEndpoint(), proofFactory) : null;
        challengeResponse = oAuth2AuthorizationServer.process(challengeRequest);
        assertEquals(challengeResponse.getAttestationChallenge(), cache.getChallenge(challengeResponse.getAttestationChallenge()).challenge());
        clientAttestationPoP = TestUtils.createClientAttestationPoP(clientMetadata.getClientId(), challengeResponse.getAttestationChallenge(), oAuth2AuthorizationServer.getConfiguration().getIssuer().toString(), clientKey);
        request = new MockRequest();
        request.addHeader("OAuth-Client-Attestation", clientAttestation.serialize());
        request.addHeader("OAuth-Client-Attestation-PoP", clientAttestationPoP.serialize());
        request.addParameter("client_id", clientMetadata.getClientId());
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", code);
        request.addParameter("redirect_uri", clientMetadata.getRedirectUris().getFirst());
        request.addParameter("code_verifier", "1234567890123456789012345678901234567890123");
        if (hasDPoP == DPopTestCase.DPOP_ALL || hasDPoP == DPopTestCase.DPOP_TOKEN) {
            request.addHeader("dpop", dPoPHeaderToken);
        }
        TokenRequest tokenRequest = new TokenRequest(request.getHeaders(), request.getParameters());
        TokenResponse tokenResponse = oAuth2AuthorizationServer.process(tokenRequest);
        assertNotNull(tokenResponse);
        assertNotNull(tokenResponse.getIdToken());
        if (hasDPoP == DPopTestCase.DPOP_ALL || hasDPoP == DPopTestCase.DPOP_TOKEN) {
            assertEquals("DPoP", tokenResponse.getTokenType());
        }else{
            assertEquals("Bearer", tokenResponse.getTokenType());
        }
        verify(auditLogger).auditTokenRequest(tokenRequest);
        verify(auditLogger).auditTokenResponse(tokenResponse);

        // 5. Validate the id_token
        JWKSet jwkSet = oAuth2AuthorizationServer.getPublicJWKSet();
        SignedJWT idToken = SignedJWT.parse(tokenResponse.getIdToken());
        JWSHeader idTokenHeader = idToken.getHeader();
        assertEquals("test-kid", idTokenHeader.getKeyID());
        JWTClaimsSet idTokenClaimsSet = idToken.getJWTClaimsSet();
        assertTrue(idToken.verify(new DefaultJWSVerifierFactory().createJWSVerifier(
                idTokenHeader,
                jwkSet.getKeyByKeyId(idTokenHeader.getKeyID()).toECKey().toPublicKey())));
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
                jwkSet.getKeyByKeyId(accessTokenHeader.getKeyID()).toECKey().toKeyPair().getPublic())));
        assertEquals(TestUtils.defaultIssuer(), accessTokenClaimsSet.getIssuer());
        assertEquals("https://api.idporten.junit/v1", accessTokenClaimsSet.getAudience().getFirst());
        assertEquals(clientMetadata.getClientId(), accessTokenClaimsSet.getClaim("client_id"));
        assertEquals("12345678901", accessTokenClaimsSet.getClaim("sub"));
        assertEquals("openid pid.mdoc", accessTokenClaimsSet.getClaim("scope"));
        assertEquals("is", accessTokenClaimsSet.getClaim("issuer_state"));
        if (hasDPoP == DPopTestCase.DPOP_ALL || hasDPoP==DPopTestCase.DPOP_TOKEN) { // TODO verify test later for only DPoP on token
            assertNotNull(accessTokenClaimsSet.getClaim("cnf"));
            assertEquals(findDpopJktFromDpopHeader(dPoPHeaderToken), ((java.util.Map<String, Object>) accessTokenClaimsSet.getClaim("cnf")).get("jkt"));
        }

        // 7. Process optional userinfo request
        request = new MockRequest();
        request.addHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());
        UserInfoRequest userInfoRequest = new UserInfoRequest(request.getHeaders(), request.getParameters());
        UserInfoResponse userInfoResponse = oAuth2AuthorizationServer.process(userInfoRequest);
        assertEquals("12345678901", userInfoResponse.getSub());
        verify(auditLogger).auditUserInfoRequest(userInfoRequest);
        verify(auditLogger).auditUserInfoResponse(userInfoResponse);

        // 8. Check all id's unique
        assertEquals(4, new HashSet(List.of(requestUri.split(":")[2], code, idTokenClaimsSet.getJWTID(), accessTokenClaimsSet.getJWTID())).size());

        // 9. Check cache empty
        assertTrue(cache.getAuthorizationRequestMap().isEmpty());
        assertTrue(cache.getCode2authorizationMap().isEmpty());
        assertFalse(cache.getAccessToken2authorizationMap().isEmpty());
        verify(auditLogger, times(2)).auditClientAuthentication(clientAuthenticationCaptor.capture());
        verifyNoMoreInteractions(auditLogger);
        assertEquals("attest_jwt_client_auth", clientAuthenticationCaptor.getValue().getTokenEndpointAuthMethod());
    }

}
