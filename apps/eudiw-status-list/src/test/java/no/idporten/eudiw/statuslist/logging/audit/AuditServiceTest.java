package no.idporten.eudiw.statuslist.logging.audit;

import no.idporten.eudiw.statuslist.service.Status;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
}