package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.logging.audit.AuditIdentifier;

public enum AuditID {

    START_VERIFICATION_REQUEST("START-VERIFICATION-REQUEST"),
    START_VERIFICATION_RESPONSE("START-VERIFICATION-RESPONSE"),
    RETRIEVE_VERIFICATION_DATA_REQUEST("RETRIEVE-VERIFICATION-DATA-REQUEST"),
    RETRIEVE_VERIFICATION_DATA_RESPONSE("RETRIEVE-VERIFICATION-DATA-RESPONSE"),
    RETRIEVE_AUTHZ_REQUEST("RETRIEVE-AUTHZ-REQUEST"),
    SEND_AUTHZ_REQUEST("SEND-AUTHZ-REQUEST"),
    RETRIEVE_AUTHZ_RESPONSE("RETRIEVE-AUTHZ-RESPONSE");

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
