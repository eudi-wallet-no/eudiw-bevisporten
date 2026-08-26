package no.idporten.eudiw.issuer.credentials.status.persistence;

import no.idporten.eudiw.issuer.credentials.status.CredentialStatusInfo;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.List;

import static no.idporten.eudiw.issuer.TestData.junitIssuerTenant;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When persisting credential status data")
@ActiveProfiles("junit")
@SpringBootTest
class CredentialStatusServiceTest {

    @Autowired
    private CredentialStatusService credentialStatusService;

    @Autowired
    private CredentialIssuanceTransactionDao issuanceTransactionDao;

    @Autowired
    private StatusListEntryDao statusListEntryDao;

    @BeforeEach
    void setUp() {
        statusListEntryDao.deleteAll();
        issuanceTransactionDao.deleteAll();
    }

    private void prepareIssuanceTransaction(String txId, String credentialConfigurationId, String tenantId) {
        issuanceTransactionDao.insertTransaction(txId, credentialConfigurationId, tenantId, System.currentTimeMillis());
    }

    @DisplayName("then status entries are stored and retrieved by tenant, transaction and credential configuration")
    @Test
    void testStoreAndRetrieveByTripleKey() {
        IssuanceTransactionId transactionId = new IssuanceTransactionId("tx-1");
        prepareIssuanceTransaction("tx-1", "pid", "junit");
        CredentialStatusInfo statusInfo = new CredentialStatusInfo(
                "junit",
                "pid",
                List.of(
                        new StatusEntry(10, URI.create("https://status.example/lists/1")),
                        new StatusEntry(11, URI.create("https://status.example/lists/1"))
                )
        );

        credentialStatusService.storeCredentialStatus(junitIssuerTenant(), transactionId, statusInfo);

        CredentialStatusInfo retrieved = credentialStatusService.retrieveCredentialStatus(junitIssuerTenant(), transactionId, "pid");
        assertAll(
                () -> assertEquals("junit", retrieved.credentialIssuerTenant()),
                () -> assertEquals("pid", retrieved.credentialConfigurationId()),
                () -> assertEquals(2, retrieved.statusEntries().size()),
                () -> assertEquals(10, retrieved.statusEntries().getFirst().idx()),
                () -> assertEquals("https://status.example/lists/1", retrieved.statusEntries().getFirst().uri().toString()),
                () -> assertEquals(11, retrieved.statusEntries().getLast().idx())
        );
    }

    @DisplayName("then storing status entries fails when issuance transaction does not exist")
    @Test
    void testStoreFailsWhenIssuanceTransactionIsMissing() {
        IssuanceTransactionId transactionId = new IssuanceTransactionId("tx-2");

        assertThrows(IllegalStateException.class, () -> credentialStatusService.storeCredentialStatus(
                junitIssuerTenant(),
                transactionId,
                new CredentialStatusInfo(
                        "junit",
                        "pid",
                        List.of(new StatusEntry(99, URI.create("https://status.example/lists/new")))
                )
        ));
    }

    @DisplayName("then created timestamp is stable and updated timestamp changes on update")
    @Test
    void testCreatedAndUpdatedTimestampLifecycle() {
        IssuanceTransactionId transactionId = new IssuanceTransactionId("tx-timestamps");
        prepareIssuanceTransaction("tx-timestamps", "pid", "junit");
        credentialStatusService.storeCredentialStatus(
                junitIssuerTenant(),
                transactionId,
                new CredentialStatusInfo(
                        "junit",
                        "pid",
                        List.of(new StatusEntry(1, URI.create("https://status.example/lists/first")))
                )
        );

        CredentialIssuanceTransactionEntity initial = issuanceTransactionDao
                .findTransaction("tx-timestamps", "pid", "junit")
                .orElseThrow();
        long createdOnInsert = initial.getCreatedMs();
        long updatedOnInsert = initial.getUpdatedMs();

        issuanceTransactionDao.updateUpdatedMs(initial.getId(), updatedOnInsert + 1000);

        CredentialIssuanceTransactionEntity updated = issuanceTransactionDao
                .findTransaction("tx-timestamps", "pid", "junit")
                .orElseThrow();

        assertAll(
                () -> assertTrue(createdOnInsert > 0),
                () -> assertTrue(updatedOnInsert >= createdOnInsert),
                () -> assertEquals(initial.getId(), updated.getId()),
                () -> assertEquals(createdOnInsert, updated.getCreatedMs()),
                () -> assertEquals(updatedOnInsert + 1000, updated.getUpdatedMs())
        );
    }

    @DisplayName("then retrieve by tenant and transaction returns stored status")
    @Test
    void testRetrieveByTransactionReturnsStoredStatus() {
        IssuanceTransactionId transactionId = new IssuanceTransactionId("tx-3");
        prepareIssuanceTransaction("tx-3", "pid", "junit");
        credentialStatusService.storeCredentialStatus(
                junitIssuerTenant(),
                transactionId,
                new CredentialStatusInfo(
                        "junit",
                        "pid",
                        List.of(new StatusEntry(1, URI.create("https://status.example/lists/pid")))
                )
        );
        CredentialStatusInfo retrieved = credentialStatusService.retrieveRevocableCredentialStatus(junitIssuerTenant(), transactionId);
        assertAll(
                () -> assertEquals("pid", retrieved.credentialConfigurationId()),
                () -> assertEquals(1, retrieved.statusEntries().getFirst().idx())
        );
    }

    @DisplayName("then a credential can only be marked revoked once")
    @Test
    void testMarkCredentialRevokedOnlyOnce() {
        IssuanceTransactionId transactionId = new IssuanceTransactionId("tx-revoked");
        prepareIssuanceTransaction("tx-revoked", "pid", "junit");

        assertTrue(credentialStatusService.markCredentialRevoked(junitIssuerTenant(), transactionId));
        assertFalse(credentialStatusService.markCredentialRevoked(junitIssuerTenant(), transactionId));
        assertNull(credentialStatusService.retrieveRevocableCredentialStatus(junitIssuerTenant(), transactionId));
    }

    @DisplayName("then missing transaction returns null")
    @Test
    void testMissingTransactionReturnsNull() {
        CredentialStatusInfo retrieved = credentialStatusService.retrieveCredentialStatus(
                junitIssuerTenant(),
                new IssuanceTransactionId("unknown"),
                "pid"
        );

        assertNull(retrieved);
    }
}
