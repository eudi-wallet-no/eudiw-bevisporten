package no.idporten.eudiw.statuslist.logging.audit;

import no.idporten.logging.audit.AuditIdentifier;

public enum AuditID {
    ALLOCATE_INDEXES("ALLOCATE_INDEXES"),
    UPDATE_STATUS("UPDATE_STATUS"),
    RETRIEVE_STATUS("RETRIEVE_STATUS");

    private static final String AUDIT_ID_FORMAT = "DL-STATUS-LIST-ISSUER-%s";

    private final String auditName;

    AuditIdentifier auditIdentifier() {
        return () -> String.format(AUDIT_ID_FORMAT, auditName);
    }
    AuditID(String auditName) {
        this.auditName = auditName;
    }
    public String getAuditName() {
        return auditName;
    }
}
