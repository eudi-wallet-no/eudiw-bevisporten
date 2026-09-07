package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.openid4vp.verifier.OpenID4VPVerifierServiceApiClient;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationRequest;
import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationResponse;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationResultResponse;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationStatusResponse;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteraction;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("When verifying OpenID4VP requests and responses")
@ExtendWith(MockitoExtension.class)
class OpenID4VPVerificationServiceTest {

    private static final String WALLET_INTERACTION_ID = "wallet-interaction-id";
    private static final String VERIFIER_TRANSACTION_ID = "verifier-transaction-id";
    private static final URI ISSUER = URI.create("https://idporten.example.com");

    @Mock
    private OpenID4VPVerifierServiceApiClient openID4VPVerifierServiceApiClient;
    @Mock
    private OpenIDConnectIntegration openIDConnectServer;
    @Mock
    private OpenID4VPVerificationHandler verificationHandler;

    private OpenID4VPVerificationService service;
    private WalletInteraction walletInteraction;

    @BeforeEach
    void setUp() {
        service = new OpenID4VPVerificationService(openID4VPVerifierServiceApiClient, openIDConnectServer);
        walletInteraction = new WalletInteraction(WALLET_INTERACTION_ID);
    }

    @DisplayName("When starting verification")
    @Nested
    class StartVerificationTests {

        private static final URI AUTHORIZATION_REQUEST = URI.create("https://verifier.eidas2sandkasse.dev/authorize");
        private static final URI AUTHORIZATION_REQUEST_QR_CODE = URI.create("https://verifier.eidas2sandkasse.dev/qr");

        private DcqlQuery dcqlQuery;
        private OpenID4VPAuthorizationRequests result;

        @BeforeEach
        void setUp() throws Exception {
            dcqlQuery = new DcqlQuery(List.of(), List.of());
            OpenIDConnectSdkConfiguration sdkConfiguration = OpenIDConnectSdkConfiguration.builder()
                    .issuer(ISSUER)
                    .build();
            lenient().when(openIDConnectServer.getSDKConfiguration()).thenReturn(sdkConfiguration);
            when(verificationHandler.createDcqlQuery(WALLET_INTERACTION_ID)).thenReturn(dcqlQuery);
            when(openID4VPVerifierServiceApiClient.startVerification(any())).thenReturn(
                    new StartVerificationResponse(AUTHORIZATION_REQUEST, AUTHORIZATION_REQUEST_QR_CODE, VERIFIER_TRANSACTION_ID));

            result = service.startVerification(verificationHandler, walletInteraction);
        }

        @DisplayName("then a start verification request is sent with the dcql query from the handler and a redirect uri based on the issuer")
        @Test
        void testStartVerificationRequest() {
            StartVerificationRequest expected = new StartVerificationRequest(
                    dcqlQuery,
                    URI.create(ISSUER + "/login/" + WALLET_INTERACTION_ID));
            verify(openID4VPVerifierServiceApiClient).startVerification(eq(expected));
        }

        @DisplayName("then the wallet interaction is updated with the verifier transaction id")
        @Test
        void testWalletInteractionUpdated() {
            assertEquals(VERIFIER_TRANSACTION_ID, walletInteraction.getVerifierTransactionId());
        }

        @DisplayName("then the authorization request and qr code are returned")
        @Test
        void testAuthorizationRequestReturned() {
            assertEquals(AUTHORIZATION_REQUEST, result.sameDeviceRequest());
            assertEquals(AUTHORIZATION_REQUEST_QR_CODE, result.crossDeviceQRCodeDataUri());
        }
    }

    @DisplayName("When completing verification")
    @Nested
    class CompleteVerificationTests {

        @BeforeEach
        void setVerifierTransactionId() {
            walletInteraction.setVerifierTransactionId(VERIFIER_TRANSACTION_ID);
        }

        @DisplayName("and a single valid credential was shared")
        @Nested
        class ValidCredentialTests {

            private VerifiedCredential verifiedCredential;
            private Authorization expectedAuthorization;

            @BeforeEach
            void setUp() throws Exception {
                verifiedCredential = new VerifiedCredential(true, Map.of("pid", "value"));
                expectedAuthorization = Authorization.builder().sub("any").build();
                when(openID4VPVerifierServiceApiClient.retrieveVerifiedCredentials(VERIFIER_TRANSACTION_ID)).thenReturn(
                        new VerificationResultResponse(VERIFIER_TRANSACTION_ID, Map.of(WALLET_INTERACTION_ID, List.of(verifiedCredential))));
                when(verificationHandler.completeVerification(verifiedCredential)).thenReturn(expectedAuthorization);

            }

            @DisplayName("then the handler completes verification with the verified credential")
            @Test
            void testHandlerCalledWithCredential() throws Exception {
                service.completeVerification(verificationHandler, walletInteraction);
                verify(verificationHandler).completeVerification(eq(verifiedCredential));
            }

            @DisplayName("then the authorization from the handler is returned")
            @Test
            void testAuthorizationReturned() throws Exception {
                Authorization result = service.completeVerification(verificationHandler, walletInteraction);
                assertSame(expectedAuthorization, result);
            }
        }

        @DisplayName("and something if not right with the result from the verifier")
        @Nested
        class InvalidCredentialTests {


            @DisplayName("then nothing shared will fail the login")
            @Test
            void testNoCredentialsSharedThrows() {
                when(openID4VPVerifierServiceApiClient.retrieveVerifiedCredentials(VERIFIER_TRANSACTION_ID)).thenReturn(
                        new VerificationResultResponse(VERIFIER_TRANSACTION_ID, Map.of()));

                InvalidVerificationException e = assertThrows(InvalidVerificationException.class,
                        () -> service.completeVerification(verificationHandler, walletInteraction));
                assertEquals("Invalid verified credentials", e.getMessage());
                verify(verificationHandler, never()).completeVerification(any());
            }

            @DisplayName("then multiple credentials shared will fail the login")
            @Test
            void testMultipleCredentialsSharedThrows() {
                VerifiedCredential credential = new VerifiedCredential(true, Map.of());
                when(openID4VPVerifierServiceApiClient.retrieveVerifiedCredentials(VERIFIER_TRANSACTION_ID)).thenReturn(
                        new VerificationResultResponse(VERIFIER_TRANSACTION_ID, Map.of(WALLET_INTERACTION_ID, List.of(credential, credential))));

                assertThrows(InvalidVerificationException.class,
                        () -> service.completeVerification(verificationHandler, walletInteraction));
                verify(verificationHandler, never()).completeVerification(any());
            }

            @DisplayName("then the shared kinvalid credential will fail the login")
            @Test
            void testInvalidCredentialThrows() {
                VerifiedCredential invalidCredential = new VerifiedCredential(false, Map.of());
                when(openID4VPVerifierServiceApiClient.retrieveVerifiedCredentials(VERIFIER_TRANSACTION_ID)).thenReturn(
                        new VerificationResultResponse(VERIFIER_TRANSACTION_ID, Map.of(WALLET_INTERACTION_ID, List.of(invalidCredential))));

                assertThrows(InvalidVerificationException.class,
                        () -> service.completeVerification(verificationHandler, walletInteraction));
                verify(verificationHandler, never()).completeVerification(any());
            }
        }
    }

    @DisplayName("When checking if verification is complete")
    @Nested
    class IsVerificationCompleteTests {

        @DisplayName("and the wallet interaction has no verifier transaction id")
        @Test
        void testNotStartedReturnsFalse() {
            assertFalse(service.isVerificationComplete(walletInteraction));
            verify(openID4VPVerifierServiceApiClient, never()).retrieveStatus(any());
        }

        @DisplayName("and the verifier reports status AVAILABLE")
        @Test
        void testStatusAvailableReturnsTrue() {
            walletInteraction.setVerifierTransactionId(VERIFIER_TRANSACTION_ID);
            when(openID4VPVerifierServiceApiClient.retrieveStatus(VERIFIER_TRANSACTION_ID)).thenReturn(
                    new VerificationStatusResponse("AVAILABLE", VERIFIER_TRANSACTION_ID));

            assertTrue(service.isVerificationComplete(walletInteraction));
        }

        @DisplayName("and the verifier reports a status other than AVAILABLE")
        @Test
        void testStatusPendingReturnsFalse() {
            walletInteraction.setVerifierTransactionId(VERIFIER_TRANSACTION_ID);
            when(openID4VPVerifierServiceApiClient.retrieveStatus(VERIFIER_TRANSACTION_ID)).thenReturn(
                    new VerificationStatusResponse("PENDING", VERIFIER_TRANSACTION_ID));

            assertFalse(service.isVerificationComplete(walletInteraction));
        }
    }
}
