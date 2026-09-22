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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @DisplayName("initializes and retrieves a waiting transaction")
    void initializesAndRetrievesTransaction() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        DcqlQuery dcqlQuery = new DcqlQuery();
        dcqlQuery.setCredentials(List.of());
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
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationTransaction retrievedTransaction =
                verificationTransactionService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);

        assertAll(
                () -> assertSame(transaction, retrievedTransaction),
                () -> assertSame(dcqlQuery, transaction.getDcqlQuery()),
                () -> assertEquals(redirectUri, transaction.getRedirectUri()),
                () -> assertSame(clientApplication, transaction.getClientApplication()),
                () -> assertEquals(VerificationTransactionService.STATUS_WAIT, transaction.getStatus()),
                () -> assertTrue(transaction.isIncludeValidationDetails()));
    }

    @Test
    @DisplayName("returns the current status and initializes a missing status")
    void handlesTransactionStatuses() {
        String transactionWithoutStatusId = "transaction-without-status";
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction availableTransaction = transaction(
                clientApplication,
                VerificationTransactionService.STATUS_AVAILABLE,
                null);
        VerificationTransaction transactionWithoutStatus = transaction(clientApplication, null, null);
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(availableTransaction);
        when(cacheService.getVerificationTransaction(clientApplication, transactionWithoutStatusId))
                .thenReturn(transactionWithoutStatus);

        String availableStatus =
                verificationTransactionService.retrieveStatus(clientApplication, VERIFIER_TRANSACTION_ID);
        String unknownStatus =
                verificationTransactionService.retrieveStatus(clientApplication, transactionWithoutStatusId);

        assertAll(
                () -> assertEquals(VerificationTransactionService.STATUS_AVAILABLE, availableStatus),
                () -> assertEquals(VerificationTransactionService.STATUS_UNKNOWN, unknownStatus),
                () -> assertEquals(
                        VerificationTransactionService.STATUS_UNKNOWN,
                        transactionWithoutStatus.getStatus()));
        verify(cacheService, never()).putVerificationTransaction(
                clientApplication,
                VERIFIER_TRANSACTION_ID,
                availableTransaction);
        verify(cacheService).putVerificationTransaction(
                clientApplication,
                transactionWithoutStatusId,
                transactionWithoutStatus);
    }

    @Test
    @DisplayName("rejects status and credential retrieval for another client application")
    void rejectsTransactionForAnotherClientApplication() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication("other-client", "other-keystore"),
                VerificationTransactionService.STATUS_AVAILABLE,
                new VerifiedCredentials(Map.of()));
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationException statusException = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveStatus(clientApplication, VERIFIER_TRANSACTION_ID));
        VerificationException credentialsException = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID));

        assertAll(
                () -> assertVerificationException(
                        statusException,
                        "client application id does not match transactions client application id"),
                () -> assertVerificationException(
                        credentialsException,
                        "client application name does not match transactions client application name"));
        verify(cacheService, never()).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    @Test
    @DisplayName("rejects adding and retrieving credentials for an unknown transaction")
    void rejectsUnknownTransaction() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(null);

        VerificationException addException = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.addVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID,
                        new VerifiedCredentials(Map.of()),
                        Map.of()));
        VerificationException retrieveException = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID));

        assertAll(
                () -> assertVerificationException(addException, "Unknown verifier transaction"),
                () -> assertVerificationException(retrieveException, "Unknown verifier transaction"));
        verify(cacheService, never()).updateVerificationTransaction(any(), anyString(), any());
        verify(cacheService, never()).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    @Test
    @DisplayName("rejects credential retrieval without a client or before data is available")
    void rejectsUnavailableCredentials() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction waitingTransaction = transaction(
                clientApplication,
                VerificationTransactionService.STATUS_WAIT,
                null);

        VerificationException clientException = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(null, VERIFIER_TRANSACTION_ID));
        verify(cacheService, never()).getVerificationTransaction(null, VERIFIER_TRANSACTION_ID);

        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(waitingTransaction);
        VerificationException dataException = assertThrows(
                VerificationException.class,
                () -> verificationTransactionService.retrieveVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID));

        assertAll(
                () -> assertVerificationException(clientException, "Unknown client application"),
                () -> assertVerificationException(dataException, "Verifier transaction data not available"));
        verify(cacheService, never()).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    @Test
    @DisplayName("adds and retrieves verified credentials")
    void addsAndRetrievesVerifiedCredentials() {
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
        VerificationTransaction retrievedTransaction =
                verificationTransactionService.retrieveVerifiedCredentials(
                        clientApplication,
                        VERIFIER_TRANSACTION_ID);

        assertAll(
                () -> assertSame(transaction, retrievedTransaction),
                () -> assertEquals(VerificationTransactionService.STATUS_AVAILABLE, transaction.getStatus()),
                () -> assertSame(verifiedCredentials, transaction.getVerifiedCredentials()),
                () -> assertSame(response, transaction.getResponse()));
        verify(cacheService).updateVerificationTransaction(
                clientApplication,
                VERIFIER_TRANSACTION_ID,
                transaction);
        verify(cacheService).removeVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID);
    }

    @Test
    @DisplayName("marks a waiting transaction as an error")
    void marksWaitingTransactionAsError() {
        ClientApplication clientApplication = clientApplication("client-id", "client-keystore");
        VerificationTransaction transaction = transaction(
                clientApplication,
                VerificationTransactionService.STATUS_WAIT,
                null);
        when(cacheService.getVerificationTransaction(clientApplication, VERIFIER_TRANSACTION_ID))
                .thenReturn(transaction);

        verificationTransactionService.markAsError(clientApplication, VERIFIER_TRANSACTION_ID);

        assertEquals(VerificationTransactionService.STATUS_ERROR, transaction.getStatus());
        verify(cacheService).updateVerificationTransaction(
                clientApplication,
                VERIFIER_TRANSACTION_ID,
                transaction);
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
