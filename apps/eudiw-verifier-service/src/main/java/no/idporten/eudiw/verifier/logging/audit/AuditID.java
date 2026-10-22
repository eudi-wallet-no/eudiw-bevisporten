package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.logging.audit.AuditIdentifier;

public enum AuditID {

    START_VERIFICATION_REQUEST("START-VERIFICATION-REQUEST");

    private static final String AUDIT_ID_FORMAT = "DL-VERIFIER-SERVICE-%s";

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
