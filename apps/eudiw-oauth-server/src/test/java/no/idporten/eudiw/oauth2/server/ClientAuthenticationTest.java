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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        public void testValidAttestation() throws Exception {
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
                    .clientId(clientId)
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
