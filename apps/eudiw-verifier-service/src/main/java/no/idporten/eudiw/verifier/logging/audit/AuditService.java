package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Qualifier("auditLogger")
    private final AuditLogger auditLogger;

    public AuditService(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

}
