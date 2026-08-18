package no.idporten.eudiw.issuer.logging.audit;

import no.idporten.logging.audit.AuditIdentifier;

public enum AuditID {

    START_CREDENTIAL_ISSUANCE("START_CREDENTIAL_ISSUANCE"),
    CREATE_CREDENTIAL_OFFER("CREATE_CREDENTIAL_OFFER"),
    ISSUE_CREDENTIAL("ISSUE_CREDENTIAL"),
    ISSUE_CREDENTIAL_STATUS("ISSUE_CREDENTIAL_STATUS"),
    REVOKE_CREDENTIAL("REVOKE_CREDENTIAL"),
    WALLET_UPDATED_STATUS("WALLET_UPDATED_STATUS");

    private static final String AUDIT_ID_FORMAT = "DL-ISSUER-SERVER-%s";

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
