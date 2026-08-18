package no.idporten.eudiw.statuslist.logging.audit;

import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditService {

    // Attributes for audit logging
    protected static final String ENTRIES = "entries";
    protected static final String COUNT = "count";
    protected static final String STATUS = "status";

    private final AuditLogger auditLogger;


    public AuditService(@Qualifier("auditLogger") AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void logAllocatedEntries(AuditableStatusEntryCollection allocatedEntries) {
        AuditLogEntries<Map<String, Object>> auditAttributes = allocatedEntries.toAudit();

        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.ALLOCATE_INDEXES.auditIdentifier())
                .logNullAttributes(false)
                .attribute(ENTRIES, auditAttributes.entries())
                .attribute(COUNT, auditAttributes.count())
                .build());
    }

    public void logUpdatedEntries(AuditableStatusEntryCollection updatedEntries, String status) {
        AuditLogEntries<Map<String, Object>> auditAttributes = updatedEntries.toAudit();

        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.UPDATE_STATUS.auditIdentifier())
                .logNullAttributes(false)
                .attribute(ENTRIES, auditAttributes.entries())
                .attribute(COUNT, auditAttributes.count())
                .attribute(STATUS, status)
                .build());
    }
}
