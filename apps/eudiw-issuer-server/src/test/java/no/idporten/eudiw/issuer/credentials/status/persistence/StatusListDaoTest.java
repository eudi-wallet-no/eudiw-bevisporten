package no.idporten.eudiw.issuer.credentials.status.persistence;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When persisting token status list")
@ActiveProfiles("junit")
@SpringBootTest
class StatusListDaoTest {

    @Autowired
    private CredentialStatusListDao credentialStatusListDao;

    @Autowired
    private WiaStatusListDao wiaStatusListDao;

    @Autowired
    private KaStatusListDao kaStatusListDao;

    @Autowired
    private CredentialIssuanceTransactionDao issuanceTransactionDao;

    @BeforeEach
    void setUp() {
        credentialStatusListDao.deleteAll();
        wiaStatusListDao.deleteAll();
        kaStatusListDao.deleteAll();
        issuanceTransactionDao.deleteAll();
    }

    @DisplayName("then credential entries can be stored and retrieved")
    @Test
    void testCredentialStatusList() {
        assertInsertAndFindEntries(credentialStatusListDao);
    }

    @DisplayName("then wallet instance attestation entries can be stored and retrieved")
    @Test
    void testWiaStatusList() {
        assertInsertAndFindEntries(wiaStatusListDao);
    }

    @DisplayName("then key attestation entries can be stored and retrieved")
    @Test
    void testKaStatusList() {
        assertInsertAndFindEntries(kaStatusListDao);
    }

    @DisplayName("then entries are kept apart in each table")
    @Test
    void testTablesAreSeparate() {
        long transactionId = issuanceTransactionDao.insertTransaction("tx-1", "pid", "junit", 1000L).getId();

        credentialStatusListDao.insertEntries(transactionId, List.of(new StatusEntry(1, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1"))));
        wiaStatusListDao.insertEntries(transactionId, List.of(new StatusEntry(2, URI.create("https://junit.wallet.eidas2sandkasse.dev/wia/1"))));
        kaStatusListDao.insertEntries(transactionId, List.of(new StatusEntry(3, URI.create("https://junit.wallet.eidas2sandkasse.dev/ka/1"))));

        assertAll(
                () -> assertEquals(1, credentialStatusListDao.findEntries(transactionId).getFirst().getIdx()),
                () -> assertEquals(2, wiaStatusListDao.findEntries(transactionId).getFirst().getIdx()),
                () -> assertEquals(3, kaStatusListDao.findEntries(transactionId).getFirst().getIdx())
        );
    }

    private void assertInsertAndFindEntries(StatusListDao dao) {
        long transactionId = issuanceTransactionDao.insertTransaction("tx-1", "pid", "junit", 1000L).getId();
        long before = System.currentTimeMillis();

        dao.insertEntries(transactionId, List.of(
                new StatusEntry(10, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1")),
                new StatusEntry(11, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1"))
        ));

        long after = System.currentTimeMillis();
        List<StatusListEntryEntity> entries = dao.findEntries(transactionId);
        assertAll(
                () -> assertEquals(2, entries.size()),
                () -> assertEquals(transactionId, entries.getFirst().getCredentialIssuanceTransactionId()),
                () -> assertEquals("https://junit.status.eidas2sandkasse.dev/lists/1", entries.getFirst().getUri()),
                () -> assertEquals(10, entries.getFirst().getIdx()),
                () -> assertTrue(entries.getFirst().getCreatedMs() >= before && entries.getFirst().getCreatedMs() <= after),
                () -> assertEquals(11, entries.getLast().getIdx()),
                () -> assertEquals(entries.getFirst().getCreatedMs(), entries.getLast().getCreatedMs())
        );
    }

}
