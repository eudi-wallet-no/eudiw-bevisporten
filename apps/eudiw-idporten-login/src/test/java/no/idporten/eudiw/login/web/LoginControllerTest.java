package no.idporten.eudiw.login.web;

import no.idporten.eudiw.login.openid4vp.OpenID4VPVerificationService;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationStatus;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteraction;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteractionService;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.AuthorizationResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.RedirectedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("When handling the wallet login flow")
@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    private static final String WALLET_INTERACTION_ID = "wallet-interaction-id";
    private static final String VERIFIER_TRANSACTION_ID = "verifier-transaction-id";

    @Mock
    private OpenIDConnectIntegration openIDConnectServer;
    @Mock
    private OpenID4VPVerificationService verificationService;
    @Mock
    private WalletInteractionService walletInteractionService;
    @Mock
    private PushedAuthorizationRequest pushedAuthorizationRequest;
    @Mock
    private AuthorizationResponse authorizationResponse;

    private LoginController controller;
    private WalletInteraction walletInteraction;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        controller = new LoginController(openIDConnectServer, verificationService, walletInteractionService);
        walletInteraction = new WalletInteraction(WALLET_INTERACTION_ID);
        walletInteraction.setVerifierTransactionId(VERIFIER_TRANSACTION_ID);
        session = new MockHttpSession();
        session.setAttribute(PushedAuthorizationRequest.class.getName(), pushedAuthorizationRequest);
        session.setAttribute("WALLET_INTERACTION_ID", WALLET_INTERACTION_ID);
        when(walletInteractionService.retrieveWalletInteraction(WALLET_INTERACTION_ID))
                .thenReturn(walletInteraction);
    }

    @DisplayName("When polling returns a terminal status, then completion is expected")
    @ParameterizedTest
    @EnumSource(value = VerificationStatus.class, names = {"AVAILABLE", "ERROR"})
    void terminalStatusCompletesPolling(VerificationStatus verificationStatus) throws Exception {
        when(verificationService.retrieveVerificationStatus(walletInteraction))
                .thenReturn(verificationStatus);

        assertEquals(200, controller.poll(WALLET_INTERACTION_ID, session).getStatusCode().value());
    }

    @DisplayName("When polling returns a non-terminal status, then continued polling is expected")
    @ParameterizedTest
    @EnumSource(value = VerificationStatus.class, names = {"WAIT", "UNKNOWN"})
    void nonTerminalStatusContinuesPolling(VerificationStatus verificationStatus) throws Exception {
        when(verificationService.retrieveVerificationStatus(walletInteraction))
                .thenReturn(verificationStatus);

        assertEquals(202, controller.poll(WALLET_INTERACTION_ID, session).getStatusCode().value());
    }

    @DisplayName("When login receives ERROR, then an access denied authorization response is expected")
    @Test
    void errorStatusReturnsAuthorizationError() throws Exception {
        when(verificationService.retrieveVerificationStatus(walletInteraction))
                .thenReturn(VerificationStatus.ERROR);
        when(openIDConnectServer.errorResponse(
                pushedAuthorizationRequest,
                "access_denied",
                "Verification with EU Digital Identity Wallet failed"))
                .thenReturn(authorizationResponse);
        when(openIDConnectServer.createClientResponse(authorizationResponse))
                .thenReturn(new RedirectedResponse(
                        "https://client.example.com/callback",
                        Map.of("error", "access_denied", "state", "client-state")));

        String result = controller.login(
                WALLET_INTERACTION_ID,
                new ExtendedModelMap(),
                session);

        assertTrue(result.startsWith("redirect:https://client.example.com/callback?"));
        assertTrue(result.contains("error=access_denied"));
        assertTrue(result.contains("state=client-state"));
        verify(verificationService, never()).completeVerification(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(walletInteractionService).removeWalletInteraction(WALLET_INTERACTION_ID);
        assertTrue(session.isInvalid());
    }
}
