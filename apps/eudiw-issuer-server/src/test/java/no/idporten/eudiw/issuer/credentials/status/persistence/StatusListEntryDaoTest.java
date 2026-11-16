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

@DisplayName("When persisting credential status list entries")
@ActiveProfiles("junit")
@SpringBootTest
class StatusListEntryDaoTest {

    @Autowired
    private StatusListEntryDao statusListEntryDao;

    @Autowired
    private CredentialIssuanceTransactionDao issuanceTransactionDao;

    @BeforeEach
    void setUp() {
        statusListEntryDao.deleteAll();
        issuanceTransactionDao.deleteAll();
    }

    @DisplayName("then entries can be stored and retrieved")
    @Test
    void testInsertAndFindEntries() {
        long transactionId = issuanceTransactionDao.insertTransaction("tx-1", "pid", "junit", 1000L).getId();
        long before = System.currentTimeMillis();

        statusListEntryDao.insertEntries(transactionId, List.of(
                new StatusEntry(10, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1")),
                new StatusEntry(11, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1"))
        ));

        long after = System.currentTimeMillis();
        List<StatusListEntryEntity> entries = statusListEntryDao.findEntries(transactionId);
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
