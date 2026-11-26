package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jwt.SignedJWT;
import net.eidas2sandkasse.tsl.TokenStatusListException;
import net.eidas2sandkasse.tsl.TokenStatusListValidator;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.util.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When validating the status of a wallet instance attestation")
class WalletInstanceAttestationStatusTest {

    @Mock
    private TokenStatusListValidator tokenStatusListValidator;

    private OpenID4VCIAuthorizationServer authorizationServer;

    @BeforeEach
    void setUp() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .tokenStatusListValidator(tokenStatusListValidator)
                .build();
        authorizationServer = new OpenID4VCIAuthorizationServer(serverConfiguration);
    }

    private SignedJWT wia(Object clientStatus) throws Exception {
        return TestUtils.createClientAttestation("wallet", "jw", "https://jw.eidas2sandkasse.dev", TestUtils.createECPrivateKey(), clientStatus);
    }

    private OpenID4VCIAuthorizationServer walletInstanceAttestationOptionalServer() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .tokenStatusListValidator(tokenStatusListValidator)
                .requireWalletInstanceAttestation(false)
                .build();
        return new OpenID4VCIAuthorizationServer(serverConfiguration);
    }

    @Test
    @DisplayName("then a missing client_status is accepted when wallet instance attestation is optional")
    void testMissingClientStatusAcceptedWhenOptional() throws Exception {
        OpenID4VCIAuthorizationServer optionalServer = walletInstanceAttestationOptionalServer();
        optionalServer.validateWalletInstanceAttestationStatus(wia(null));
        verifyNoInteractions(tokenStatusListValidator);
    }

    @Test
    @DisplayName("then an incomplete client_status is rejected also when wallet instance attestation is optional")
    void testIncompleteClientStatusRejectedWhenOptional() throws Exception {
        OpenID4VCIAuthorizationServer optionalServer = walletInstanceAttestationOptionalServer();
        SignedJWT wia = wia(TestUtils.clientStatus(TestUtils.defaultClientStatusListUri().toString(), null));
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> optionalServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                () -> assertTrue(e.errorDescription().contains("JWT missing client_status.status.status_list.idx claim"))
        );
        verifyNoInteractions(tokenStatusListValidator);
    }

    @Test
    @DisplayName("then a revoked wallet instance is rejected also when wallet instance attestation is optional")
    void testRevokedStatusRejectedWhenOptional() throws Exception {
        when(tokenStatusListValidator.validate(any(), anyLong())).thenReturn(false);
        OpenID4VCIAuthorizationServer optionalServer = walletInstanceAttestationOptionalServer();
        SignedJWT wia = wia(TestUtils.defaultClientStatus());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> optionalServer.validateWalletInstanceAttestationStatus(wia));
        assertTrue(e.errorDescription().contains("Wallet instance is revoked"));
    }

    @Test
    @DisplayName("then a wallet instance with valid status in client_status is accepted")
    void testValidStatus() throws Exception {
        when(tokenStatusListValidator.validate(any(), anyLong())).thenReturn(true);
        authorizationServer.validateWalletInstanceAttestationStatus(wia(TestUtils.defaultClientStatus()));
        verify(tokenStatusListValidator).validate(TestUtils.defaultClientStatusListUri(), TestUtils.defaultClientStatusListIdx());
    }

    @Test
    @DisplayName("then a revoked wallet instance raises an OAuth2 error with code invalid_client_attestation")
    void testRevokedStatus() throws Exception {
        when(tokenStatusListValidator.validate(any(), anyLong())).thenReturn(false);
        SignedJWT wia = wia(TestUtils.defaultClientStatus());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                () -> assertEquals(401, e.getHttpStatusCode()),
                () -> assertTrue(e.errorDescription().contains("Wallet instance is revoked"))
        );
    }

    @Test
    @DisplayName("then a missing client_status raises an OAuth2 error with code invalid_client_attestation")
    void testMissingClientStatus() throws Exception {
        SignedJWT wia = wia(null);
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                () -> assertTrue(e.errorDescription().contains("JWT missing client_status.status.status_list.uri claim"))
        );
        verifyNoInteractions(tokenStatusListValidator);
    }

    @Test
    @DisplayName("then a client_status without idx raises an OAuth2 error with code invalid_client_attestation")
    void testMissingIdx() throws Exception {
        SignedJWT wia = wia(TestUtils.clientStatus(TestUtils.defaultClientStatusListUri().toString(), null));
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                () -> assertTrue(e.errorDescription().contains("JWT missing client_status.status.status_list.idx claim"))
        );
        verifyNoInteractions(tokenStatusListValidator);
    }

    @Test
    @DisplayName("then a client_status with invalid structure raises an OAuth2 error with code invalid_client_attestation")
    void testInvalidClientStatusStructure() throws Exception {
        Map<String, Object> clientStatus = JsonObjectBuilder.builder().addAttribute("status", "not-an-object").build();
        SignedJWT wia = wia(clientStatus);
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                () -> assertTrue(e.errorDescription().contains("JWT invalid client_status claim"))
        );
        verifyNoInteractions(tokenStatusListValidator);
    }

    @Test
    @DisplayName("then a client_status with idx of invalid type raises an OAuth2 error with code invalid_client_attestation")
    void testInvalidIdxType() throws Exception {
        SignedJWT wia = wia(TestUtils.clientStatus(TestUtils.defaultClientStatusListUri().toString(), true));
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                () -> assertTrue(e.errorDescription().contains("JWT invalid client_status claim"))
        );
        verifyNoInteractions(tokenStatusListValidator);
    }

    @Test
    @DisplayName("then a client_status rejected by the status list validator raises an OAuth2 error with code invalid_client_attestation")
    void testStatusListReferenceRejected() throws Exception {
        when(tokenStatusListValidator.validate(any(), anyLong())).thenThrow(new IllegalArgumentException("Status list uri must be https"));
        SignedJWT wia = wia(TestUtils.defaultClientStatus());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.INVALID_CLIENT_ATTESTATION, e.error()),
                () -> assertTrue(e.errorDescription().contains("Invalid client_status claim"))
        );
    }

    @Test
    @DisplayName("then an unavailable status list raises an OAuth2 error with code temporarily_unavailable")
    void testStatusListUnavailable() throws Exception {
        when(tokenStatusListValidator.validate(any(), anyLong())).thenThrow(new TokenStatusListException("Could not resolve token status list status", null));
        SignedJWT wia = wia(TestUtils.defaultClientStatus());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateWalletInstanceAttestationStatus(wia));
        assertAll(
                () -> assertEquals(OAuth2Exception.TEMPORARILY_UNAVAILABLE, e.error()),
                () -> assertEquals(503, e.getHttpStatusCode())
        );
    }

    @Test
    @DisplayName("then client authentication with attestation checks client_status")
    void testClientAuthenticationChecksClientStatus() throws Exception {
        when(tokenStatusListValidator.validate(any(), anyLong())).thenReturn(false);
        SignedJWT wia = wia(TestUtils.defaultClientStatus());
        OAuth2Exception e = assertThrows(OAuth2Exception.class, () -> authorizationServer.validateClientAttestation(wia));
        assertTrue(e.errorDescription().contains("Wallet instance is revoked"));
    }

}
