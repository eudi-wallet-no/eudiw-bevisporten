package no.idporten.eudiw.statuslist.logging.audit;

import jakarta.validation.constraints.NotNull;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    // Attributes for audit logging
    protected static final String INDEXES = "indexes";
    protected static final String STATUS = "status";

    private final AuditLogger auditLogger;


    public AuditService(@Qualifier("auditLogger") AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void logAllocateIndexes(@NotNull List<Integer> indexesAllocated) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.ALLOCATE_INDEXES.auditIdentifier())
                .logNullAttributes(false)
                .attribute(INDEXES, indexesAllocated.toString())
                .build());
    }

    public void logUpdateIndexes(@NotNull List<Integer> indexesUpdated, @NotNull String statusUpdated) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.UPDATE_STATUS.auditIdentifier())
                .logNullAttributes(false)
                .attribute(INDEXES, indexesUpdated.toString())
                .attribute(STATUS, statusUpdated)
                .build());
    }


}
