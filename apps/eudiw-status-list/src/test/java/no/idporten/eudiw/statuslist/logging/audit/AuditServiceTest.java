package no.idporten.eudiw.statuslist.logging.audit;

import no.idporten.eudiw.statuslist.domain.StatusEntry;
import no.idporten.eudiw.statuslist.issuer.api.StatusEntryUpdateRequest;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
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
    void logAllocatedEntries() {
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net/{id}", 1);

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

            List<?> arrayNode = (List<?>) argument.getAttributes().get(AuditService.ENTRIES);
            for (Object node : arrayNode) {
                Map<?, ?> map = (Map<?, ?>) node;
                assertEquals(uri.toString(), map.get("uri"));
            }

            return true;
        }));

    }

    @Test
    void logUpdatedEntries() {
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", 1);

        List<StatusEntryUpdateRequest> entries = List.of(
                new StatusEntryUpdateRequest(31432, uri, "INVALID"),
                new StatusEntryUpdateRequest(982315, uri, "INVALID")
        );

        auditService.logUpdatedEntries(new AuditEntryCollection<>(entries));

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(argument -> {
            assertNotNull(argument);
            assertEquals(AuditID.UPDATE_STATUS.auditIdentifier().auditId(), argument.getAuditId().auditId());
            assertFalse(argument.isLogNullAttributes());
            assertTrue(argument.getAttributes().containsKey(AuditService.ENTRIES));
            assertEquals(new AuditEntryCollection<>(entries).toAudit(), argument.getAttributes().get(AuditService.ENTRIES));

            List<?> arrayNode = (List<?>) argument.getAttributes().get(AuditService.ENTRIES);
            for (Object node : arrayNode) {
                Map<?, ?> map = (Map<?, ?>) node;
                assertEquals(uri.toString(), map.get("uri"));
                assertEquals("INVALID", map.get("status"));
            }

            return true;
        }));

    }
}