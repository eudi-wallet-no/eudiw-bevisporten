package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.eudiw.verifier.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Qualifier("auditLogger")
    private final AuditLogger auditLogger;

    // Attributes for audit logging

    protected static final String VERIFIER_TRANSACTION_ID = "verifier_transaction_id";
    protected static final String REQUEST_ID = "request_id";
    protected static final String CLIENT_APPLICATION_ID = "client_application_id";
    protected static final String DCQL_QUERY = "dcql_query";
    protected static final String REDIRECT_URI = "redirect_uri";

    public AuditService(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void auditStartVerificationRequest(StartVerificationRequest startVerificationRequest, ClientApplication clientApplication, String verifierTransactionId, String requestId) {

        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.START_VERIFICATION_REQUEST.auditIdentifier())
                .attribute(VERIFIER_TRANSACTION_ID, verifierTransactionId)
                .attribute(REQUEST_ID, requestId)
                .attribute(CLIENT_APPLICATION_ID, clientApplication.getId())
                .attribute(DCQL_QUERY, startVerificationRequest.dcqlQuery())
                .attribute(REDIRECT_URI, startVerificationRequest.redirectUri().toString())
                .build();

        auditLogger.log(auditEntry);
    }
}
