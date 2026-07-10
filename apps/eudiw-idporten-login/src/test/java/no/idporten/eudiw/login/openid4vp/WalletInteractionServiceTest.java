package no.idporten.eudiw.login.openid4vp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("When managing wallet interactions")
@ExtendWith(MockitoExtension.class)
class WalletInteractionServiceTest {

    @Mock
    private WalletInteractionCache walletInteractionCache;
    private  WalletInteractionProperties walletInteractionProperties;
    private WalletInteractionService service;

    @BeforeEach
    void setUp() {
        walletInteractionProperties = new WalletInteractionProperties(
                Duration.ofSeconds(5),
                Duration.ofMinutes(5)
        );
        service = new WalletInteractionService(walletInteractionCache, walletInteractionProperties);
    }

    @DisplayName("When creating a new wallet interaction")
    @Nested
    class CreateWalletInteractionTests {

        private WalletInteraction walletInteraction;
        private long beforeCreate;

        @BeforeEach
        void create() {
            beforeCreate = System.currentTimeMillis();
            walletInteraction = service.createWalletInteraction();
        }

        @DisplayName("then the interaction is stored in the cache")
        @Test
        void testStoredInCache() {
            verify(walletInteractionCache).putWalletInteraction(eq(walletInteraction.getId()), eq(walletInteraction));
        }

        @DisplayName("then the id is a non-blank UUID string")
        @Test
        void testIdIsSet() {
            assertNotNull(walletInteraction.getId());
            assertFalse(walletInteraction.getId().isBlank());
        }

        @DisplayName("then createdAtEpochMillis is set to approximately now")
        @Test
        void testCreatedAtIsSetToNow() {
            assertTrue(walletInteraction.createdAtEpochMillis() >= beforeCreate);
            assertTrue(walletInteraction.createdAtEpochMillis() <= System.currentTimeMillis());
        }

        @DisplayName("then expiresAtEpochMillis is set to n minutes after creation")
        @Test
        void testExpiresAtIsTenMinutesAfterCreation() {
            long expectedExpiry = walletInteraction.createdAtEpochMillis() + (5 * 60 * 1000L);
            assertEquals(expectedExpiry, walletInteraction.expiresAtEpochMillis());
        }

        @DisplayName("then the interaction is not yet started")
        @Test
        void testNotStarted() {
            assertFalse(walletInteraction.isStarted());
        }

        @DisplayName("then verifierTransactionId is not set")
        @Test
        void testVerifierTransactionIdIsNull() {
            assertNull(walletInteraction.getVerifierTransactionId());
        }
    }

    @DisplayName("When updating a wallet interaction with a verifier transaction id")
    @Nested
    class UpdateWalletInteractionTests {

        private static final String VERIFIER_TRANSACTION_ID = "verifier-txn-123";
        private WalletInteraction walletInteraction;

        @BeforeEach
        void update() {
            walletInteraction = service.createWalletInteraction();
            walletInteraction.setVerifierTransactionId(VERIFIER_TRANSACTION_ID);
            service.updateWalletInteraction(walletInteraction);
        }

        @DisplayName("then the updated interaction is stored in the cache")
        @Test
        void testStoredInCache() {
            verify(walletInteractionCache, times(2)).putWalletInteraction(eq(walletInteraction.getId()), any());
        }

        @DisplayName("then verifierTransactionId is set")
        @Test
        void testVerifierTransactionIdIsSet() {
            assertEquals(VERIFIER_TRANSACTION_ID, walletInteraction.getVerifierTransactionId());
        }

        @DisplayName("then the interaction is marked as started")
        @Test
        void testIsStarted() {
            assertTrue(walletInteraction.isStarted());
        }
    }

    @DisplayName("When retrieving a wallet interaction")
    @Nested
    class RetrieveWalletInteractionTests {

        private static final String INTERACTION_ID = "some-id";

        @DisplayName("then the interaction is fetched from the cache by id")
        @Test
        void testFetchedFromCache() {
            WalletInteraction expected = new WalletInteraction(INTERACTION_ID);
            when(walletInteractionCache.getWalletInteraction(INTERACTION_ID)).thenReturn(expected);

            WalletInteraction result = service.retrieveWalletInteraction(INTERACTION_ID);

            assertSame(expected, result);
            verify(walletInteractionCache).getWalletInteraction(INTERACTION_ID);
        }
    }

    @DisplayName("When removing a wallet interaction")
    @Nested
    class RemoveWalletInteractionTests {

        private static final String INTERACTION_ID = "some-id";

        @DisplayName("then the interaction is removed from the cache by id")
        @Test
        void testRemovedFromCache() {
            service.removeWalletInteraction(INTERACTION_ID);
            verify(walletInteractionCache).removeWalletInteraction(INTERACTION_ID);
        }
    }
}
