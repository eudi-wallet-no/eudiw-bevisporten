package no.idporten.eudiw.issuer.issuance.authz;

import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionDao;
import no.idporten.eudiw.issuer.credentials.status.persistence.StatusListEntryDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("When persisting subject credential transactions")
@ActiveProfiles("junit")
@SpringBootTest
class SubjectCredentialTransactionDaoTest {

    @Autowired
    private SubjectCredentialTransactionDao dao;

    @Autowired
    private CredentialIssuanceTransactionDao issuanceTransactionDao;

    @Autowired
    private StatusListEntryDao statusListEntryDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        statusListEntryDao.deleteAll();
        dao.deleteAll();
        issuanceTransactionDao.deleteAll();
    }

    @DisplayName("then inserted subject credential transaction is stored")
    @Test
    void insertSubjectCredentialTransaction() {
        long now = System.currentTimeMillis();
        issuanceTransactionDao.insertTransaction("tx-1", "pid", "junit", now);

        dao.insertSubjectCredentialTransaction("12345678901", "tx-1", now);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subject_credential_transaction WHERE subject_identifier = ? AND issuance_transaction_id = ?",
                Integer.class,
                "12345678901",
                "tx-1"
        );

        Long createdMs = jdbcTemplate.queryForObject(
                "SELECT created_ms FROM subject_credential_transaction WHERE subject_identifier = ? AND issuance_transaction_id = ?",
                Long.class,
                "12345678901",
                "tx-1"
        );

        assertAll(
                () -> assertEquals(1, count),
                () -> assertNotNull(createdMs),
                () -> assertEquals(now, createdMs)
        );
    }

    @DisplayName("then joined transactions are retrievable by subject, credential configuration and tenant")
    @Test
    void findBySubjectAndTypeReturnsMatchingTransactions() {
        long now = System.currentTimeMillis();

        issuanceTransactionDao.insertTransaction("tx-match", "pid", "junit", now);
        issuanceTransactionDao.updateStatus("tx-match", "offer_issued", now + 1);
        issuanceTransactionDao.updateNotificationId("tx-match", "notif-match", now + 2);
        dao.insertSubjectCredentialTransaction("12345678901", "tx-match", now);

        issuanceTransactionDao.insertTransaction("tx-other-config", "other-config", "junit", now + 10);
        dao.insertSubjectCredentialTransaction("12345678901", "tx-other-config", now + 10);

        issuanceTransactionDao.insertTransaction("tx-other-subject", "pid", "junit", now + 20);
        dao.insertSubjectCredentialTransaction("10987654321", "tx-other-subject", now + 20);

        issuanceTransactionDao.insertTransaction("tx-other-tenant", "pid", "other-tenant", now + 30);
        dao.insertSubjectCredentialTransaction("12345678901", "tx-other-tenant", now + 30);

        List<SubjectCredentialIssuanceTransactionEntity> transactions = dao
                .findBySubjectAndType("12345678901", "pid", "junit");

        assertEquals(1, transactions.size());

        SubjectCredentialIssuanceTransactionEntity transaction = transactions.getFirst();
        assertAll(
                () -> assertNotNull(transaction.getSubjectCredentialTransactionId()),
                () -> assertEquals("12345678901", transaction.getSubjectIdentifier()),
                () -> assertEquals("tx-match", transaction.getIssuanceTransactionId()),
                () -> assertEquals(now, transaction.getSubjectCredentialTransactionCreatedMs()),
                () -> assertNotNull(transaction.getCredentialIssuanceTransactionId()),
                () -> assertEquals("pid", transaction.getCredentialConfigurationId()),
                () -> assertEquals("junit", transaction.getCredentialIssuerTenant()),
                () -> assertEquals(now, transaction.getCredentialIssuanceTransactionCreatedMs()),
                () -> assertEquals(now + 2, transaction.getCredentialIssuanceTransactionUpdatedMs()),
                () -> assertEquals("offer_issued", transaction.getCredentialIssuanceTransactionStatus()),
                () -> assertEquals("notif-match", transaction.getCredentialIssuanceTransactionNotificationId())
        );
    }

}
