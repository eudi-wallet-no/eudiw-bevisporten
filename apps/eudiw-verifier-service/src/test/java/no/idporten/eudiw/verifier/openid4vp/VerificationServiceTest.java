package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.api.verification.StartVerificationResponse;
import no.idporten.eudiw.verifier.api.verification.VerificationResultResponse;
import no.idporten.eudiw.verifier.api.verification.VerificationStatusResponse;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When using the verification service")
class VerificationServiceTest {

    private static final String VERIFIER_TRANSACTION_ID = "transaction-id";

    @Mock
    private OpenID4VPRequestService openID4VPRequestService;

    @Mock
    private VerificationTransactionService verificationTransactionService;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(openID4VPRequestService, verificationTransactionService);
    }

    @Test
    @DisplayName("starts a verification for same-device and cross-device flows")
    void startsVerification() throws Exception {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        DcqlQuery dcqlQuery = new DcqlQuery();
        URI redirectUri = URI.create("https://client.example/callback");
        StartVerificationRequest request = new StartVerificationRequest(dcqlQuery, redirectUri);
        URI sameDeviceAuthorizationRequest = URI.create("eudi-openid4vp://same-device");
        URI crossDeviceAuthorizationRequest = URI.create("eudi-openid4vp://cross-device");
        URI qrCodeDataUri = URI.create("data:image/png;base64,cross-device");

        when(openID4VPRequestService.createRequestId(eq(clientApplication), anyString()))
                .thenReturn("request-id");
        when(openID4VPRequestService.createAuthorizationRequest("request-id", clientApplication, "same_device"))
                .thenReturn(sameDeviceAuthorizationRequest);
        when(openID4VPRequestService.createAuthorizationRequest("request-id", clientApplication, "cross_device"))
                .thenReturn(crossDeviceAuthorizationRequest);
        when(openID4VPRequestService.createQrCodeDataURI(crossDeviceAuthorizationRequest))
                .thenReturn(qrCodeDataUri);

        StartVerificationResponse response = verificationService.startVerification(request, clientApplication, true);

        assertEquals(sameDeviceAuthorizationRequest, response.authorizationRequest());
        assertEquals(qrCodeDataUri, response.authorizationRequestQRCode());
        assertDoesNotThrow(() -> UUID.fromString(response.verifierTransactionId()));
        verify(openID4VPRequestService).createRequestId(clientApplication, response.verifierTransactionId());
        verify(openID4VPRequestService).createQrCodeDataURI(crossDeviceAuthorizationRequest);
        verify(verificationTransactionService).initTransaction(
                dcqlQuery,
                redirectUri,
                response.verifierTransactionId(),
                clientApplication,
                true);
    }

    @Test
    @DisplayName("returns the status for a verification transaction")
    void returnsVerificationStatus() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        when(verificationTransactionService.retrieveStatus(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(VerificationTransactionService.STATUS_WAIT);

        VerificationStatusResponse response =
                verificationService.verifierStatus(VERIFIER_TRANSACTION_ID, clientApplication);

        assertEquals(VerificationTransactionService.STATUS_WAIT, response.status());
        assertEquals(VERIFIER_TRANSACTION_ID, response.verifierTransactionId());
    }

    @Test
    @DisplayName("returns verified credentials with the existing response data")
    void returnsVerificationDataWithExistingResponse() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerifiedCredentials verifiedCredentials = new VerifiedCredentials(Map.of());
        Map<String, Object> request = Map.of("request", "value");
        Map<String, Object> response = Map.of("response", "value");
        VerificationTransaction transaction = transaction(
                clientApplication,
                verifiedCredentials,
                request,
                response,
                URI.create("https://client.example/callback"),
                new DcqlQuery());
        when(verificationTransactionService.retrieveVerifiedCredentials(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationResultResponse result =
                verificationService.retrieveVerificationData(VERIFIER_TRANSACTION_ID, clientApplication);

        assertEquals(VERIFIER_TRANSACTION_ID, result.verifierTransactionId());
        assertSame(verifiedCredentials.credentials(), result.credentials());
        assertSame(request, result.authorizationRequest());
        assertSame(response, result.authorizationResponse());
    }

    @Test
    @DisplayName("creates response data when the transaction has no response")
    void createsMissingResponseData() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        DcqlQuery dcqlQuery = new DcqlQuery();
        URI redirectUri = URI.create("https://client.example/callback");
        VerificationTransaction transaction = transaction(
                clientApplication,
                new VerifiedCredentials(Map.of()),
                Map.of("request", "value"),
                null,
                redirectUri,
                dcqlQuery);
        when(verificationTransactionService.retrieveVerifiedCredentials(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationResultResponse result =
                verificationService.retrieveVerificationData(VERIFIER_TRANSACTION_ID, clientApplication);

        Map<String, Object> expectedResponse = Map.of(
                "response_uri", redirectUri.toString(),
                "client_id", clientApplication.getId(),
                "dcql_query", dcqlQuery);
        assertEquals(expectedResponse, result.authorizationResponse());
        assertSame(result.authorizationResponse(), transaction.getResponse());
    }

    private static VerificationTransaction transaction(
            ClientApplication clientApplication,
            VerifiedCredentials verifiedCredentials,
            Map<String, Object> request,
            Map<String, Object> response,
            URI redirectUri,
            DcqlQuery dcqlQuery) {
        VerificationTransaction transaction = new VerificationTransaction();
        transaction.setClientApplication(clientApplication);
        transaction.setVerifiedCredentials(verifiedCredentials);
        transaction.setRequest(request);
        transaction.setResponse(response);
        transaction.setRedirectUri(redirectUri);
        transaction.setDcqlQuery(dcqlQuery);
        return transaction;
    }

    private static ClientApplication clientApplication(String id, String keystoreName) {
        ClientApplication clientApplication = new ClientApplication();
        clientApplication.setId(id);
        clientApplication.setKeystoreName(keystoreName);
        return clientApplication;
    }
}
