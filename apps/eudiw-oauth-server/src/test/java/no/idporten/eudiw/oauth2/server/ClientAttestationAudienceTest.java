package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.AuthenticatedRequest;
import no.idporten.eudiw.oauth2.server.protocol.Challenge;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import no.idporten.eudiw.oauth2.server.protocol.TokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When authenticating with client attestation PoP, then request-specific audience validation is expected")
class ClientAttestationAudienceTest {

    private static final String ISSUER = "https://oauth.eidas2sandkasse.dev";
    private static final String PAR_ENDPOINT = ISSUER + "/par";
    private static final String TOKEN_ENDPOINT = ISSUER + "/token";
    private static final String CLIENT_ID = "wallet-client";
    private static final String CHALLENGE = "audience-test-challenge";

    private OAuth2AuthorizationServerBase authorizationServer;

    @BeforeEach
    void setUp() throws Exception {
        authorizationServer = createServer(true);
    }

    @ParameterizedTest
    @MethodSource("acceptedAudiences")
    @DisplayName("When PoP targets the issuer or the requested endpoint, then successful authentication is expected")
    void acceptsIssuerOrRequestEndpoint(boolean parRequest, String audience, boolean configureEndpoints) throws Exception {
        authorizationServer = createServer(configureEndpoints);
        AuthenticatedRequest request = createRequest(parRequest, List.of(audience));

        assertEquals(CLIENT_ID, authorizationServer.authenticateClient(request).getClientId());
        assertAll(
                () -> assertEquals(CLIENT_ID, request.getClientId()),
                () -> assertNull(request.getClientAttestation()),
                () -> assertNull(request.getClientAttestationPoP())
        );
    }

    private static Stream<Arguments> acceptedAudiences() {
        return Stream.of(
                Arguments.of(true, ISSUER, true),
                Arguments.of(true, PAR_ENDPOINT, true),
                Arguments.of(false, ISSUER, true),
                Arguments.of(false, TOKEN_ENDPOINT, true),
                Arguments.of(true, ISSUER, false),
                Arguments.of(false, ISSUER, false)
        );
    }

    @ParameterizedTest
    @MethodSource("rejectedAudiences")
    @DisplayName("When PoP has an invalid audience, then invalid_client without consuming the challenge is expected")
    void rejectsInvalidAudience(boolean parRequest, List<String> audiences, String expectedDescription) throws Exception {
        AuthenticatedRequest request = createRequest(parRequest, audiences);

        OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(request));

        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT, exception.error()),
                () -> assertTrue(exception.errorDescription().contains(expectedDescription)),
                () -> assertNotNull(authorizationServer.getConfiguration().getCache().getChallenge(CHALLENGE))
        );
    }

    private static Stream<Arguments> rejectedAudiences() {
        return Stream.of(true, false).flatMap(parRequest -> Stream.of(
                Arguments.of(parRequest, List.of(parRequest ? TOKEN_ENDPOINT : PAR_ENDPOINT), "Unknown JWT audience"),
                Arguments.of(parRequest, List.of("https://unknown.eidas2sandkasse.dev"), "Unknown JWT audience"),
                Arguments.of(parRequest, List.of(ISSUER + "/"), "Unknown JWT audience"),
                Arguments.of(parRequest, List.of(), "Missing JWT audience"),
                Arguments.of(parRequest, null, "Missing JWT audience"),
                Arguments.of(parRequest, List.of(ISSUER, parRequest ? PAR_ENDPOINT : TOKEN_ENDPOINT), "Unique JWT audience required")
        ));
    }

    @ParameterizedTest
    @MethodSource("unconfiguredEndpoints")
    @DisplayName("When the requested endpoint is not configured, then its URI as audience is rejected")
    void rejectsUnconfiguredEndpoint(boolean parRequest, String audience) throws Exception {
        authorizationServer = createServer(false);
        AuthenticatedRequest request = createRequest(parRequest, List.of(audience));

        OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(request));

        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT, exception.error()),
                () -> assertTrue(exception.errorDescription().contains("Unknown JWT audience"))
        );
    }

    private static Stream<Arguments> unconfiguredEndpoints() {
        return Stream.of(
                Arguments.of(true, PAR_ENDPOINT),
                Arguments.of(false, TOKEN_ENDPOINT)
        );
    }

    private OAuth2AuthorizationServerBase createServer(boolean configureEndpoints) throws Exception {
        OAuth2ServerConfiguration configuration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .issuer(URI.create(ISSUER))
                .pushedAuthorizationRequestEndpoint(configureEndpoints ? URI.create(PAR_ENDPOINT) : null)
                .tokenEndpoint(configureEndpoints ? URI.create(TOKEN_ENDPOINT) : null)
                .build();
        return new OAuth2AuthorizationServerBase(configuration);
    }

    private AuthenticatedRequest createRequest(boolean parRequest, List<String> audiences) throws Exception {
        ECKey clientKey = TestUtils.createECPrivateKey();
        SignedJWT attestation = TestUtils.createClientAttestation(CLIENT_ID, "wallet", "https://wallet.eidas2sandkasse.dev", clientKey);
        SignedJWT proof = TestUtils.createClientAttestationPoP(CLIENT_ID, CHALLENGE, ISSUER, clientKey);
        SignedJWT proofWithAudience = new SignedJWT(proof.getHeader(),
                new JWTClaimsSet.Builder(proof.getJWTClaimsSet()).audience(audiences).build());
        proofWithAudience.sign(new ECDSASigner(clientKey));
        authorizationServer.getConfiguration().getCache().putChallenge(new Challenge(CHALLENGE, 120));
        Map<String, List<String>> headers = Map.of(
                "OAuth-Client-Attestation", List.of(attestation.serialize()),
                "OAuth-Client-Attestation-PoP", List.of(proofWithAudience.serialize())
        );
        return parRequest
                ? new PushedAuthorizationRequest(headers, Map.of())
                : new TokenRequest(headers, Map.of());
    }
}
