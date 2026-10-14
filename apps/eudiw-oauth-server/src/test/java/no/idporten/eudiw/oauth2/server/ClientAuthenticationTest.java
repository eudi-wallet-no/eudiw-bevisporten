package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.oauth2.server.audit.OpenIDConnectAuditLogger;
import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.AuditData;
import no.idporten.eudiw.oauth2.server.protocol.AuthenticatedRequest;
import no.idporten.eudiw.oauth2.server.protocol.Challenge;
import no.idporten.eudiw.oauth2.server.protocol.ClientAuthentication;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import no.idporten.eudiw.oauth2.server.protocol.TokenRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("When authenticating a client")
public class ClientAuthenticationTest {

    @Mock
    private OpenIDConnectAuditLogger auditLogger;
    @Captor
    private ArgumentCaptor<ClientAuthentication> clientAuthenticationCaptor;

    private OAuth2AuthorizationServerBase authorizationServer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRequest implements AuthenticatedRequest {

        private String clientId;
        private String clientAttestation;
        private String clientAttestationPoP;

        @Override
        public void setAuthenticatedClientId(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public void clearAuthentication() {

        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .client(ClientMetadata.builder().clientId("anotherclient").scope("openid").redirectUri("https://junit.idporten.no/").build())
                .tokenEndpoint(URI.create("https://oauth.eidas2sandkasse.dev/token"))
                .pushedAuthorizationRequestEndpoint(URI.create("https://oauth.eidas2sandkasse.dev/par"))
                .cache(new SimpleOpenIDConnectCache())
                .auditLogger(auditLogger)
                .build();
        authorizationServer = new OAuth2AuthorizationServerBase(serverConfiguration);
    }

    @DisplayName("with invalid use of client authentication")
    @Nested
    class InvalidUseOfClientAuthenticationTests {

        @Test
        @DisplayName("then missing client authentication raises an OAuth2 error with code invalid_client")
        public void testMissingClientAuthentication() {
            AuthenticatedRequest authenticatedRequest = new TestRequest();
            OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Missing client authentication"))
            );
            verifyNoInteractions(auditLogger);
        }

    }

    @DisplayName("using an attestation (wia, attest_jwt_client_auth)")
    @Nested
    class AttestationBasedTests {

        @Test
        @DisplayName("then a valid client attestation and pop is accepted and audit logged")
        public void testValidAttestationWithX5cHeader() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            final Challenge challenge = new Challenge("dgsdjhagdhjaj", 120);
            authorizationServer.getConfiguration().getCache().putChallenge(challenge);
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoP(
                    clientId,
                    challenge.challenge(),
                    authorizationServer.getConfiguration().getIssuer().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = TestRequest.builder()
                    .clientAttestation(clientAttestation.serialize())
                    .clientAttestationPoP(clientAttestationPoPJwt.serialize())
                    .build();
            ClientMetadata clientMetadata = authorizationServer.authenticateClient(authenticatedRequest);
            assertEquals(clientId, clientMetadata.getClientId());
            verify(auditLogger).auditClientAuthentication(clientAuthenticationCaptor.capture());
            AuditData auditData = clientAuthenticationCaptor.getValue().getAuditData();
            assertEquals(clientId, auditData.getAttribute("client_id"));
            assertEquals("attest_jwt_client_auth", auditData.getAttribute("token_endpoint_auth_method"));
            assertEquals(clientAttestation.serialize(), auditData.getAttribute("client_attestation"));
            assertNotEquals(clientAttestationPoPJwt.serialize(), auditData.getAttribute("client_attestation_pop"));
            assertTrue(auditData.getAttribute("client_attestation_pop").toString().endsWith("..."));
            assertEquals(challenge.challenge(), auditData.getAttribute("attestation_challenge"));
            assertEquals("w", auditData.getAttribute("wallet_name"));
            assertEquals("https://w.eidas2sandkasse.dev", auditData.getAttribute("wallet_link"));
        }

        @Test
        @DisplayName("then the token endpoint uri is valid as PoP audience for the token endpoint")
        public void testValidAttestationWithTokenEndpointAudience() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            final Challenge challenge = new Challenge("dgsdjhagdhjaj", 120);
            authorizationServer.getConfiguration().getCache().putChallenge(challenge);
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoP(
                    clientId,
                    challenge.challenge(),
                    authorizationServer.getConfiguration().getTokenEndpoint().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = TokenRequest.builder()
                    .clientAttestation(clientAttestation.serialize())
                    .clientAttestationPoP(clientAttestationPoPJwt.serialize())
                    .build();
            ClientMetadata clientMetadata = authorizationServer.authenticateClient(authenticatedRequest);
            assertEquals(clientId, clientMetadata.getClientId());
            verify(auditLogger).auditClientAuthentication(clientAuthenticationCaptor.capture());
            AuditData auditData = clientAuthenticationCaptor.getValue().getAuditData();
            assertAll(
                    () -> assertEquals(clientId, auditData.getAttribute("client_id")),
                    () -> assertEquals("attest_jwt_client_auth", auditData.getAttribute("token_endpoint_auth_method")),
                    () -> assertEquals(challenge.challenge(), auditData.getAttribute("attestation_challenge"))
            );
        }

        @Test
        @DisplayName("then the token endpoint uri is not a valid PoP audience for the par endpoint")
        public void testAttestationWithTokenEndpointAudienceRejectedForParRequest() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            final Challenge challenge = new Challenge("dgsdjhagdhjaj", 120);
            authorizationServer.getConfiguration().getCache().putChallenge(challenge);
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoP(
                    clientId,
                    challenge.challenge(),
                    authorizationServer.getConfiguration().getTokenEndpoint().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = new PushedAuthorizationRequest(
                    Map.of(
                            "OAuth-Client-Attestation", List.of(clientAttestation.serialize()),
                            "OAuth-Client-Attestation-PoP", List.of(clientAttestationPoPJwt.serialize())),
                    Map.of("client_id", List.of(clientId)));
            OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Unknown JWT audience"))
            );
            verifyNoInteractions(auditLogger);
        }

        @Test
        @DisplayName("then a client attestation missing the x5c header is rejected")
        public void testAttestationMissingX5cHeader() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            final Challenge challenge = new Challenge("dgsdjhagdhjaj", 120);
            authorizationServer.getConfiguration().getCache().putChallenge(challenge);
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey, false);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoP(
                    clientId,
                    challenge.challenge(),
                    authorizationServer.getConfiguration().getIssuer().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = TestRequest.builder()
                    .clientId(clientId)
                    .clientAttestation(clientAttestation.serialize())
                    .clientAttestationPoP(clientAttestationPoPJwt.serialize())
                    .build();
            OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                    () -> assertTrue(e.errorDescription().contains("Invalid client authentication. JWT missing x5c header"))
            );
        }

        @Test
        @DisplayName("then a client attestation with an unknown challenge returns use attestation challenge with a fresh challenge")
        public void testAttestationWithUnknownChallengeReturnsUseAttestationChallenge() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoP(
                    clientId,
                    "unknown-challenge",
                    authorizationServer.getConfiguration().getIssuer().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = TestRequest.builder()
                    .clientId(clientId)
                    .clientAttestation(clientAttestation.serialize())
                    .clientAttestationPoP(clientAttestationPoPJwt.serialize())
                    .build();
            UseAttestationChallengeOAuth2Exception exception = assertThrows(UseAttestationChallengeOAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertInstanceOf(UseAttestationChallengeOAuth2Exception.class, exception),
                    () -> assertEquals(OAuth2Exception.USE_ATTESTATION_CHALLENGE, exception.error()),
                    () -> assertTrue(exception.errorDescription().contains("Unknown challenge in attestation pop")),
                    () -> assertNotNull(exception.getAttestationChallenge()),
                    () -> assertNotNull(authorizationServer.getConfiguration().getCache().getChallenge(exception.getAttestationChallenge()))
            );
       }

        @Test
        @DisplayName("then a client attestation without a challenge returns use attestation challenge with a fresh challenge")
        public void testAttestationWithoutChallengeReturnsUseAttestationChallenge() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoPWithoutChallenge(
                    clientId,
                    authorizationServer.getConfiguration().getIssuer().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = TestRequest.builder()
                    .clientId(clientId)
                    .clientAttestation(clientAttestation.serialize())
                    .clientAttestationPoP(clientAttestationPoPJwt.serialize())
                    .build();
            UseAttestationChallengeOAuth2Exception exception = assertThrows(UseAttestationChallengeOAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertInstanceOf(UseAttestationChallengeOAuth2Exception.class, exception),
                    () -> assertEquals(OAuth2Exception.USE_ATTESTATION_CHALLENGE, exception.error()),
                    () -> assertTrue(exception.errorDescription().contains("Missing challenge in attestation pop")),
                    () -> assertNotNull(exception.getAttestationChallenge()),
                    () -> assertNotNull(authorizationServer.getConfiguration().getCache().getChallenge(exception.getAttestationChallenge()))
            );
        }

        @Test
        @DisplayName("then a client attestation with an expired challenge returns use attestation challenge with a fresh challenge")
        public void testAttestationWithExpiredChallengeReturnsUseAttestationChallenge() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            String challengeString = "dgsdjhagdhjaj";
            final Challenge challenge = new Challenge(challengeString, -1);
            authorizationServer.getConfiguration().getCache().putChallenge(challenge);
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoP(
                    clientId,
                    challengeString,
                    authorizationServer.getConfiguration().getIssuer().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = TestRequest.builder()
                    .clientId(clientId)
                    .clientAttestation(clientAttestation.serialize())
                    .clientAttestationPoP(clientAttestationPoPJwt.serialize())
                    .build();
            UseAttestationChallengeOAuth2Exception exception = assertThrows(UseAttestationChallengeOAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertInstanceOf(UseAttestationChallengeOAuth2Exception.class, exception),
                    () -> assertEquals(OAuth2Exception.USE_ATTESTATION_CHALLENGE, exception.error()),
                    () -> assertTrue(exception.errorDescription().contains("Expired challenge in attestation pop")),
                    () -> assertNotNull(exception.getAttestationChallenge()),
                    () -> assertNotNull(authorizationServer.getConfiguration().getCache().getChallenge(exception.getAttestationChallenge()))
            );
        }

        @Test
        @DisplayName("then attestation without challenge is accepted when challenge is optional")
        public void testAttestationWithoutChallengeAcceptedWhenChallengeOptional() throws Exception {
            final String clientId = "eudiw-abca";
            final ECKey clientKey = TestUtils.createECPrivateKey();
            OAuth2ServerConfiguration optionalChallengeServerConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                    .client(ClientMetadata.builder().clientId("anotherclient").scope("openid").redirectUri("https://junit.idporten.no/").build())
                    .cache(new SimpleOpenIDConnectCache())
                    .auditLogger(auditLogger)
                    .requireChallenge(false)
                    .build();
            OAuth2AuthorizationServerBase optionalChallengeServer = new OAuth2AuthorizationServerBase(optionalChallengeServerConfiguration);
            SignedJWT clientAttestation = TestUtils.createClientAttestation(clientId, "w", "https://w.eidas2sandkasse.dev", clientKey);
            SignedJWT clientAttestationPoPJwt = TestUtils.createClientAttestationPoPWithoutChallenge(
                    clientId,
                    optionalChallengeServer.getConfiguration().getIssuer().toString(),
                    clientKey);
            AuthenticatedRequest authenticatedRequest = TestRequest.builder()
                    .clientAttestation(clientAttestation.serialize())
                    .clientAttestationPoP(clientAttestationPoPJwt.serialize())
                    .build();

            ClientMetadata clientMetadata = optionalChallengeServer.authenticateClient(authenticatedRequest);

            assertEquals(clientId, clientMetadata.getClientId());
            verify(auditLogger).auditClientAuthentication(clientAuthenticationCaptor.capture());
            assertNull(clientAuthenticationCaptor.getValue().getAttestationChallenge());
        }
    }

    @DisplayName("without using client authentication (none)")
    @Nested
    class NoneTests {

        @Test
        public void testValidUnauthenticatedRequest() throws Exception {
            final String clientId = "eudiw-abca";
            AuthenticatedRequest authenticatedRequest = TestRequest.builder()
                    .clientId(clientId)
                    .build();
            ClientMetadata clientMetadata = authorizationServer.authenticateClient(authenticatedRequest);
            assertEquals(clientId, clientMetadata.getClientId());
            verify(auditLogger).auditClientAuthentication(clientAuthenticationCaptor.capture());
            assertEquals(clientId, clientAuthenticationCaptor.getValue().getClientId());
            assertEquals("none", clientAuthenticationCaptor.getValue().getTokenEndpointAuthMethod());
        }

    }

}
