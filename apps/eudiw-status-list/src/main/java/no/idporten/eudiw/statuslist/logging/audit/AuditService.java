package no.idporten.eudiw.statuslist.logging.audit;

import jakarta.validation.constraints.NotNull;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    // Attributes for audit logging
    protected static final String ENTRIES = "entries";

    private final AuditLogger auditLogger;


    public AuditService(@Qualifier("auditLogger") AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public <T extends Auditable> void logAllocatedEntries(@NotNull AuditEntryCollection<T> allocatedEntries) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.ALLOCATE_INDEXES.auditIdentifier())
                .logNullAttributes(false)
                .attribute(ENTRIES,  allocatedEntries.toAudit())
                .build());
    }

    public <T extends Auditable> void logUpdatedEntries(@NotNull AuditEntryCollection<T> updatedEntries) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.UPDATE_STATUS.auditIdentifier())
                .logNullAttributes(false)
                .attribute(ENTRIES, updatedEntries.toAudit())
                .build());
    }

}
