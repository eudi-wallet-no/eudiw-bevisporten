package no.idporten.eudiw.statuslist.logging.audit;

import no.idporten.eudiw.statuslist.domain.StatusEntry;
import no.idporten.eudiw.statuslist.issuer.api.StatusEntryUpdateRequest;
import no.idporten.eudiw.statuslist.service.Status;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

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
    void logAllocateIndexes() {
        List<Integer> indexes = List.of(99,200,3);
        auditService.logAllocateIndexes(indexes);
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.ALLOCATE_INDEXES.auditIdentifier().auditId(), argument.getAuditId().auditId());
            assertFalse(argument.isLogNullAttributes());
            assertTrue(argument.getAttributes().containsKey(AuditService.INDEXES));
            assertEquals(indexes.toString(), argument.getAttributes().get(AuditService.INDEXES));
            return true;
        }));
    }

    @Test
    void logUpdateIndexes() {
        List<Integer> indexes = List.of(1000000,1,44,9999);
        auditService.logUpdateIndexes(indexes, Status.getStatus(Status.INVALID));
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.UPDATE_STATUS.auditIdentifier().auditId(), argument.getAuditId().auditId());
            assertFalse(argument.isLogNullAttributes());
            assertTrue(argument.getAttributes().containsKey(AuditService.INDEXES));
            assertEquals(indexes.toString(), argument.getAttributes().get(AuditService.INDEXES));
            return true;
        }));
    }

    @Test
    void logAllocatedEntries() {
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net", "1");

        List<StatusEntry> entries = List.of(
                new StatusEntry(31432, uri),
                new StatusEntry(982315, uri)
        );

        auditService.logAllocatedEntries(new AuditEntryCollection<>(entries));

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.ALLOCATE_INDEXES.auditIdentifier().auditId(), argument.getAuditId().auditId());
            assertFalse(argument.isLogNullAttributes());
            assertTrue(argument.getAttributes().containsKey(AuditService.ENTRIES));
            assertEquals(new AuditEntryCollection<>(entries).toAudit(), argument.getAttributes().get(AuditService.ENTRIES));
            return true;
        }));

    }

    @Test
    void logUpdatedEntries() {
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", "1");

        List<StatusEntryUpdateRequest> entries = List.of(
                new StatusEntryUpdateRequest(31432, uri, "INVALID"),
                new StatusEntryUpdateRequest(982315, uri, "INVALID")
        );

        auditService.logUpdatedEntries(new AuditEntryCollection<>(entries), "INVALID");

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.UPDATE_STATUS.auditIdentifier().auditId(), argument.getAuditId().auditId());
            assertFalse(argument.isLogNullAttributes());
            assertTrue(argument.getAttributes().containsKey(AuditService.ENTRIES));
            assertEquals("INVALID", argument.getAttributes().get(AuditService.STATUS));
            assertEquals(new AuditEntryCollection<>(entries).toAudit(), argument.getAttributes().get(AuditService.ENTRIES));
            return true;
        }));

    }
}