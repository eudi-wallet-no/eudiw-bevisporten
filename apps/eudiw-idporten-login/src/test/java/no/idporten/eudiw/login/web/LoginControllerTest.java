package no.idporten.eudiw.login.web;

import no.idporten.eudiw.login.openid4vp.OpenID4VPVerificationService;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationStatus;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteraction;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteractionService;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.AuthorizationResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.RedirectedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.ExtendedModelMap;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("When handling the wallet login flow")
@WebMvcTest(LoginController.class)
class LoginControllerTest {

    private static final String WALLET_INTERACTION_ID = "wallet-interaction-id";
    private static final String VERIFIER_TRANSACTION_ID = "verifier-transaction-id";

    @MockitoBean
    private OpenIDConnectIntegration openIDConnectServer;
    @MockitoBean
    private OpenID4VPVerificationService verificationService;
    @MockitoBean
    private WalletInteractionService walletInteractionService;
    private PushedAuthorizationRequest pushedAuthorizationRequest;
    private AuthorizationResponse authorizationResponse;

    @Autowired
    private LoginController controller;
    @Autowired
    private MockMvc mockMvc;
    private WalletInteraction walletInteraction;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        pushedAuthorizationRequest = mock(PushedAuthorizationRequest.class);
        authorizationResponse = mock(AuthorizationResponse.class);
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

    @DisplayName("When login receives ERROR, then an error page is returned")
    @Test
    void errorStatusDisplaysErrorPage() throws Exception {
        when(verificationService.retrieveVerificationStatus(walletInteraction))
                .thenReturn(VerificationStatus.ERROR);

        ExtendedModelMap model = new ExtendedModelMap();
        String result = controller.login(
                WALLET_INTERACTION_ID,
                model,
                session);

        assertAll(
                () -> assertEquals("verification-error", result),
                () -> assertEquals(VERIFIER_TRANSACTION_ID, model.getAttribute("verifierTransactionId")),
                () -> assertFalse(session.isInvalid()),
                () -> assertSame(pushedAuthorizationRequest,
                        session.getAttribute(PushedAuthorizationRequest.class.getName())),
                () -> assertEquals(WALLET_INTERACTION_ID, session.getAttribute("WALLET_INTERACTION_ID")));
        verifyNoInteractions(openIDConnectServer);
        verify(verificationService, never()).completeVerification(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(walletInteractionService).removeWalletInteraction(WALLET_INTERACTION_ID);
    }

    @DisplayName("When verification fails, then a reference and back link without polling are expected")
    @Test
    void errorPageDisplaysReferenceAndLinksToCancelWithoutPolling() throws Exception {
        when(verificationService.retrieveVerificationStatus(walletInteraction))
                .thenReturn(VerificationStatus.ERROR);

        mockMvc.perform(get("/login/{walletInteractionId}", WALLET_INTERACTION_ID).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-error"))
                .andExpect(content().string(containsString("<h1>Innloggingen kunne ikke fullføres</h1>")))
                .andExpect(content().string(containsString("Feilreferanse (verifier transaction) ved kontakt med support:")))
                .andExpect(content().string(containsString("<span>%s</span>".formatted(VERIFIER_TRANSACTION_ID))))
                .andExpect(content().string(containsString(
                        "<a href=\"/cancel\" class=\"button button--secondary\">Tilbake</a>")))
                .andExpect(content().string(not(containsString("waitForWallet"))))
                .andExpect(content().string(not(containsString("fetch("))));
        assertFalse(session.isInvalid());
    }

    @DisplayName("When cancelling after ERROR, then the existing cancellation response and invalidated session are expected")
    @Test
    void cancelAfterErrorReturnsAuthorizationError() throws Exception {
        when(verificationService.retrieveVerificationStatus(walletInteraction))
                .thenReturn(VerificationStatus.ERROR);
        when(openIDConnectServer.errorResponse(
                pushedAuthorizationRequest,
                "access_denied",
                "User cancelled authentication with EU Digital Identity Wallet"))
                .thenReturn(authorizationResponse);
        when(openIDConnectServer.createClientResponse(authorizationResponse))
                .thenReturn(new RedirectedResponse(
                        "https://client.eidas2sandkasse.dev/callback",
                        Map.of("error", "access_denied", "state", "client-state")));

        assertEquals("verification-error",
                controller.login(WALLET_INTERACTION_ID, new ExtendedModelMap(), session));
        String result = controller.cancel(session);

        assertAll(
                () -> assertTrue(result.startsWith("redirect:https://client.eidas2sandkasse.dev/callback?")),
                () -> assertTrue(result.contains("error=access_denied")),
                () -> assertTrue(result.contains("state=client-state")),
                () -> assertTrue(session.isInvalid()));
    }

    @DisplayName("When login receives AVAILABLE, then a successful redirect and session cleanup are expected")
    @Test
    void availableStatusCompletesLogin() throws Exception {
        Authorization authorization = mock(Authorization.class);
        when(verificationService.retrieveVerificationStatus(walletInteraction))
                .thenReturn(VerificationStatus.AVAILABLE);
        when(pushedAuthorizationRequest.getResolvedAcrValue()).thenReturn("eidas-loa-high");
        when(verificationService.completeVerification(any(), eq(walletInteraction))).thenReturn(authorization);
        when(openIDConnectServer.authorize(pushedAuthorizationRequest, authorization))
                .thenReturn(authorizationResponse);
        when(openIDConnectServer.createClientResponse(authorizationResponse))
                .thenReturn(new RedirectedResponse(
                        "https://client.eidas2sandkasse.dev/callback",
                        Map.of("code", "authorization-code", "state", "client-state")));

        String result = controller.login(WALLET_INTERACTION_ID, new ExtendedModelMap(), session);

        assertAll(
                () -> assertTrue(result.startsWith("redirect:https://client.eidas2sandkasse.dev/callback?")),
                () -> assertTrue(result.contains("code=authorization-code")),
                () -> assertTrue(result.contains("state=client-state")),
                () -> assertTrue(session.isInvalid()));
        verify(walletInteractionService).removeWalletInteraction(WALLET_INTERACTION_ID);
    }
}
