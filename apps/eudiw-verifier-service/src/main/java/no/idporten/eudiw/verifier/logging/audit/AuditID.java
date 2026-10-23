package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.logging.audit.AuditIdentifier;

public enum AuditID {

    START_VERIFICATION_REQUEST("START-VERIFICATION-REQUEST"),
    START_VERIFICATION_RESPONSE("START-VERIFICATION-RESPONSE"),
    FETCH_VERIFICATION_DATA_REQUEST("FETCH-VERIFICATION-DATA-REQUEST"),
    FETCH_VERIFICATION_DATA_RESPONSE("FETCH-VERIFICATION-DATA-RESPONSE"),
    FETCH_AUTHZ_REQUEST("FETCH-AUTHZ-REQUEST"),
    FETCH_AUTHZ_RESPONSE("FETCH-AUTHZ-RESPONSE"),
    VP_TOKEN_AUTHZ_RESPONSE("VP-TOKEN-AUTHZ-RESPONSE");

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
