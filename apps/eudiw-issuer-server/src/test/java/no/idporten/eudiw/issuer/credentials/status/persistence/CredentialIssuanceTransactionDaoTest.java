package no.idporten.eudiw.issuer.credentials.status.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When persisting credential issuance transactions")
@ActiveProfiles("junit")
@SpringBootTest
class CredentialIssuanceTransactionDaoTest {

    @Autowired
    private CredentialIssuanceTransactionDao dao;

    @Autowired
    private StatusListEntryDao statusListEntryDao;

    @BeforeEach
    void setUp() {
        statusListEntryDao.deleteAll();
        dao.deleteAll();
    }

    @DisplayName("then inserted transaction is retrievable by all key variants")
    @Test
    void insertTransactionAndFind() {
        long now = System.currentTimeMillis();
        dao.insertTransaction("tx-1", "pid", "junit", now);

        assertAll(
                () -> assertTrue(dao.findTransaction("tx-1", "pid", "junit").isPresent()),
                () -> assertTrue(dao.findByIssuanceTransactionId("tx-1", "junit").isPresent()),
                () -> assertTrue(dao.findByIssuanceTransactionId("tx-1").isPresent())
        );
    }

    @DisplayName("then status is stored and updated")
    @Test
    void updateStatus() {
        long now = System.currentTimeMillis();
        dao.insertTransaction("tx-status", "pid", "junit", now);
        dao.updateStatus("tx-status", "offer_issued", now);

        CredentialIssuanceTransactionEntity entity = dao.findByIssuanceTransactionId("tx-status").orElseThrow();
        assertEquals("offer_issued", entity.getStatus());

        dao.updateStatus("tx-status", "credential_issued", now + 1);
        assertEquals("credential_issued", dao.findByIssuanceTransactionId("tx-status").orElseThrow().getStatus());
    }

    @DisplayName("then notification_id is stored and lookup works")
    @Test
    void updateAndLookupNotificationId() {
        long now = System.currentTimeMillis();
        dao.insertTransaction("tx-notif", "pid", "junit", now);
        dao.updateNotificationId("tx-notif", "notif-abc", now);

        CredentialIssuanceTransactionEntity found = dao.findByNotificationId("notif-abc").orElseThrow();
        assertEquals("tx-notif", found.getIssuanceTransactionId());
    }

    @DisplayName("then lookup returns empty for unknown notification_id")
    @Test
    void lookupUnknownNotificationId() {
        assertTrue(dao.findByNotificationId("does-not-exist").isEmpty());
    }

}
