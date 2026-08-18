package no.idporten.eudiw.statuslist.logging.audit;

import no.idporten.eudiw.statuslist.domain.StatusEntry;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.buildUri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("Audit log format for allocation on a single list")
    void logAllocatedEntries() {
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net/{id}", 1);

        List<StatusEntry> entries = List.of(
                new StatusEntry(31432, uri),
                new StatusEntry(982315, uri)
        );

        auditService.logAllocatedEntries(new AuditableStatusEntryCollection(entries));

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.ALLOCATE_INDEXES.auditIdentifier().auditId(), argument.getAuditId().auditId());
            assertFalse(argument.isLogNullAttributes());
            assertTrue(argument.getAttributes().containsKey("entries"));

            Map<?, ?> attributes = argument.getAttributes();
            List<?> entryList = (List<?>) attributes.get("entries");

            for (Object entry : entryList) {
                Map<?, ?> entryMap = (Map<?, ?>) entry;
                List<?> indexes = (List<?>) entryMap.get("indexes");
                assertEquals(2, indexes.size());
                assertTrue(indexes.contains(31432));
                assertTrue(indexes.contains(982315));
                assertEquals(2, entryMap.get("count"));
                assertEquals(uri.toString(), entryMap.get("uri"));
            }

            assertNull(attributes.get("status"));
            assertEquals(2, attributes.get("count"));


            return true;
        }));

    }

    @Test
    @DisplayName("Audit log format for status update on a single list")
    void logUpdatedEntries() {
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", 1);

        List<StatusEntry> entries = List.of(
                new StatusEntry(31432, uri),
                new StatusEntry(982315, uri)
        );

        auditService.logUpdatedEntries(new AuditableStatusEntryCollection(entries), "INVALID");

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.UPDATE_STATUS.auditIdentifier().auditId(), argument.getAuditId().auditId());
            assertFalse(argument.isLogNullAttributes());
            assertTrue(argument.getAttributes().containsKey("entries"));

            Map<?, ?> attributes = argument.getAttributes();
            List<?> entryList = (List<?>) attributes.get("entries");

            for (Object entry : entryList) {
                Map<?, ?> entryMap = (Map<?, ?>) entry;
                List<?> indexes = (List<?>) entryMap.get("indexes");
                assertEquals(2, indexes.size());
                assertTrue(indexes.contains(31432));
                assertTrue(indexes.contains(982315));
                assertEquals(2, entryMap.get("count"));
                assertEquals(uri.toString(), entryMap.get("uri"));
            }

            assertEquals("INVALID", attributes.get("status"));
            assertEquals(2, attributes.get("count"));

            return true;
        }));
    }

    @Test
    @DisplayName("Audit log format for allocation on multiple lists")
    void auditLogFormatForAllocation() {
        // FORMAT
        //"entries": [
        //    {"uri": https://status.junit.eidas2sandkasse.net/lists/1, "indexes": [1, 5, 2, 20], "count": 4},
        //    {"uri": https://status.junit.eidas2sandkasse.net/lists/2, "indexes": [1, 3], "count": 2}
        //],
        // "count": 6,
        URI uri1 = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", 1);
        URI uri2 = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", 2);

        List<StatusEntry> entries = List.of(
                new StatusEntry(1, uri1),
                new StatusEntry(5, uri1),
                new StatusEntry(2, uri1),
                new StatusEntry(20, uri1),
                new StatusEntry(1, uri2),
                new StatusEntry(3, uri2)
        );

        auditService.logAllocatedEntries(new AuditableStatusEntryCollection(entries));

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.ALLOCATE_INDEXES.auditIdentifier().auditId(), argument.getAuditId().auditId());

            Map<?, ?> attributes = argument.getAttributes();

            assertEquals(6, attributes.get("count"));

            assertFalse(attributes.containsKey("status"));

            List<?> entryList = (List<?>) attributes.get("entries");
            assertEquals(2, entryList.size());

            List<?> sortedEntries = entryList.stream()
                    .map(e -> (Map<?, ?>) e)
                    .sorted(Comparator.comparing(e -> ((String) e.get("uri"))))
                    .toList();

            Map<?, ?> list1Entry = (Map<?, ?>) sortedEntries.get(0);
            assertEquals(uri1.toString(), list1Entry.get("uri"));
            assertEquals(4, list1Entry.get("count"));
            List<?> list1Indexes = (List<?>) list1Entry.get("indexes");
            assertEquals(4, list1Indexes.size());
            assertTrue(list1Indexes.containsAll(List.of(1, 5, 2, 20)));

            Map<?, ?> list2Entry = (Map<?, ?>) sortedEntries.get(1);
            assertEquals(uri2.toString(), list2Entry.get("uri"));
            assertEquals(2, list2Entry.get("count"));
            List<?> list2Indexes = (List<?>) list2Entry.get("indexes");
            assertEquals(2, list2Indexes.size());
            assertTrue(list2Indexes.containsAll(List.of(1, 3)));

            return true;
        }));
    }

    @Test
    @DisplayName("Audit log format for status update on multiple lists")
    void auditLogFormat() {
        // FORMAT
        //"entries": [
        //    {"uri": https://status.junit.eidas2sandkasse.net/lists/1, "indexes": [1, 5, 2, 20], "count": 4},
        //    {"uri": https://status.junit.eidas2sandkasse.net/lists/2, "indexes": [1, 3], "count": 2}
        //],
        // "count": 6,
        // "status": "INVALID"
        URI uri1 = buildUri("https://status.eidas2sandkasse.net/lists/{id}", 1);
        URI uri2 = buildUri("https://status.eidas2sandkasse.net/lists/{id}", 2);

        List<StatusEntry> entries = List.of(
                new StatusEntry(1, uri1),
                new StatusEntry(5, uri1),
                new StatusEntry(2, uri1),
                new StatusEntry(20, uri1),
                new StatusEntry(1, uri2),
                new StatusEntry(3, uri2)
        );

        auditService.logUpdatedEntries(new AuditableStatusEntryCollection(entries), "INVALID");

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.UPDATE_STATUS.auditIdentifier().auditId(), argument.getAuditId().auditId());

            Map<?, ?> attributes = argument.getAttributes();

            assertEquals(6, attributes.get("count"));
            assertEquals("INVALID", attributes.get("status"));

            List<?> entryList = (List<?>) attributes.get("entries");
            assertEquals(2, entryList.size());

            List<?> sortedEntries = entryList.stream()
                    .map(e -> (Map<?, ?>) e)
                    .sorted(Comparator.comparing(e -> ((String) e.get("uri"))))
                    .toList();

            Map<?, ?> list1Entry = (Map<?, ?>) sortedEntries.get(0);
            assertEquals(uri1.toString(), list1Entry.get("uri"));
            assertEquals(4, list1Entry.get("count"));
            List<?> list1Indexes = (List<?>) list1Entry.get("indexes");
            assertEquals(4, list1Indexes.size());
            assertTrue(list1Indexes.containsAll(List.of(1, 5, 2, 20)));

            Map<?, ?> list2Entry = (Map<?, ?>) sortedEntries.get(1);
            assertEquals(uri2.toString(), list2Entry.get("uri"));
            assertEquals(2, list2Entry.get("count"));
            List<?> list2Indexes = (List<?>) list2Entry.get("indexes");
            assertEquals(2, list2Indexes.size());
            assertTrue(list2Indexes.containsAll(List.of(1, 3)));

            return true;
        }));
    }
}