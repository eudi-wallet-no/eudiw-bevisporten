package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.cache.CacheService;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When managing verification transactions")
class VerificationTransactionServiceTest {

    private static final String VERIFIER_TRANSACTION_ID = "transaction-id";

    @Mock
    private CacheService cacheService;

    private VerificationTransactionService verificationTransactionService;

    @BeforeEach
    void setUp() {
        verificationTransactionService = new VerificationTransactionService(cacheService);
    }

    @Test
    @DisplayName("initializes a waiting transaction in the cache")
    void initializesTransaction() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        DcqlQuery dcqlQuery = new DcqlQuery();
        dcqlQuery.setCredentials(List.of("claim1"));
        URI redirectUri = URI.create("https://client.example/callback");
        ArgumentCaptor<VerificationTransaction> transactionCaptor =
                ArgumentCaptor.forClass(VerificationTransaction.class);

        verificationTransactionService.initTransaction(
                dcqlQuery,
                redirectUri,
                VERIFIER_TRANSACTION_ID,
                clientApplication,
                true);

        verify(cacheService).putVerificationTransaction(
                eq(clientApplication),
                eq(VERIFIER_TRANSACTION_ID),
                transactionCaptor.capture());
        VerificationTransaction transaction = transactionCaptor.getValue();
        assertSame(dcqlQuery, transaction.getDcqlQuery());
        assertEquals(redirectUri, transaction.getRedirectUri());
        assertSame(clientApplication, transaction.getClientApplication());
        assertEquals(VerificationTransactionService.STATUS_WAIT, transaction.getStatus());
        assertTrue(transaction.isIncludeValidationDetails());
    }

    @Test
    @DisplayName("returns a transaction from the cache")
    void returnsTransactionFromCache() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(clientApplication, "WAIT", null);
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationTransaction result =
                verificationTransactionService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);

        assertSame(transaction, result);
    }

    @Test
    @DisplayName("returns the current transaction status")
    void returnsCurrentStatus() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication,
                VerificationTransactionService.STATUS_AVAILABLE,
                null);
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        String status = verificationTransactionService.retrieveStatus(clientApplication, VERIFIER_TRANSACTION_ID);

        assertEquals(VerificationTransactionService.STATUS_AVAILABLE, status);
        verify(cacheService, never()).putVerificationTransaction(
                clientApplication,
                VERIFIER_TRANSACTION_ID,
                transaction);
    }

    @Test
    @DisplayName("sets an unknown status when the transaction has no status")
    void setsUnknownStatus() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(clientApplication, null, null);
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        String status = verificationTransactionService.retrieveStatus(clientApplication, VERIFIER_TRANSACTION_ID);

        assertEquals(VerificationTransactionService.STATUS_UNKNOWN, status);
        assertEquals(VerificationTransactionService.STATUS_UNKNOWN, transaction.getStatus());
        verify(cacheService).putVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID, transaction);
    }

    @Test
    @DisplayName("rejects status retrieval for another client application")
    void rejectsStatusForAnotherClientApplication() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication("other-client", "other-keystore"),
                VerificationTransactionService.STATUS_WAIT,
                null);
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveStatus(clientApplication, VERIFIER_TRANSACTION_ID));

        assertVerificationException(
                exception,
                "client application id does not match transactions client application id");
    }

    @Test
    @DisplayName("rejects adding credentials to an unknown transaction")
    void rejectsAddingCredentialsToUnknownTransaction() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(null);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.addVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID,
                        new VerifiedCredentials(Map.of()),
                        Map.of()));

        assertVerificationException(exception, "Unknown verifier transaction");
        verify(cacheService, never()).updateVerificationTransaction(
                any(),
                anyString(),
                any());
    }

    @Test
    @DisplayName("adds verified credentials and makes the transaction available")
    void addsVerifiedCredentials() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication,
                VerificationTransactionService.STATUS_WAIT,
                null);
        VerifiedCredentials verifiedCredentials = new VerifiedCredentials(Map.of());
        Map<String, Object> response = Map.of("vp_token", "value");
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        verificationTransactionService.addVerifiedCredentials(
                clientApplication,
                VERIFIER_TRANSACTION_ID,
                verifiedCredentials,
                response);

        assertEquals(VerificationTransactionService.STATUS_AVAILABLE, transaction.getStatus());
        assertSame(verifiedCredentials, transaction.getVerifiedCredentials());
        assertSame(response, transaction.getResponse());
        verify(cacheService).updateVerificationTransaction(
                clientApplication,
                VERIFIER_TRANSACTION_ID,
                transaction);
    }

    @Test
    @DisplayName("rejects credential retrieval without a client application")
    void rejectsCredentialRetrievalWithoutClientApplication() {
        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(null, VERIFIER_TRANSACTION_ID));

        assertVerificationException(exception, "Unknown client application");
        verifyNoInteractions(cacheService);
    }

    @Test
    @DisplayName("rejects credential retrieval for an unknown transaction")
    void rejectsCredentialRetrievalForUnknownTransaction() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(null);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID));

        assertVerificationException(exception, "Unknown verifier transaction");
        verify(cacheService, never()).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    @Test
    @DisplayName("rejects credential retrieval before data is available")
    void rejectsCredentialRetrievalBeforeDataIsAvailable() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication,
                VerificationTransactionService.STATUS_WAIT,
                null);
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID));

        assertVerificationException(exception, "Verifier transaction data not available");
        verify(cacheService, never()).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    @Test
    @DisplayName("rejects credential retrieval for another client application")
    void rejectsCredentialRetrievalForAnotherClientApplication() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication("other-client", "other-keystore"),
                VerificationTransactionService.STATUS_AVAILABLE,
                new VerifiedCredentials(Map.of()));
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID));

        assertVerificationException(
                exception,
                "client application name does not match transactions client application name");
        verify(cacheService, never()).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    @Test
    @DisplayName("returns verified credentials and removes the transaction")
    void returnsVerifiedCredentialsAndRemovesTransaction() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication,
                VerificationTransactionService.STATUS_AVAILABLE,
                new VerifiedCredentials(Map.of()));
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationTransaction result = verificationTransactionService.retrieveVerifiedCredentials(
                clientApplication,
                VERIFIER_TRANSACTION_ID);

        assertSame(transaction, result);
        verify(cacheService).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    private static void assertVerificationException(VerificationException exception, String description) {
        assertEquals("invalid_request", exception.getError());
        assertEquals(description, exception.getErrorDescription());
    }

    private static VerificationTransaction transaction(
            ClientApplication clientApplication,
            String status,
            VerifiedCredentials verifiedCredentials) {
        VerificationTransaction transaction = new VerificationTransaction();
        transaction.setClientApplication(clientApplication);
        transaction.setStatus(status);
        transaction.setVerifiedCredentials(verifiedCredentials);
        return transaction;
    }

    private static ClientApplication clientApplication(String id, String keystoreName) {
        ClientApplication clientApplication = new ClientApplication();
        clientApplication.setId(id);
        clientApplication.setKeystoreName(keystoreName);
        return clientApplication;
    }
}
