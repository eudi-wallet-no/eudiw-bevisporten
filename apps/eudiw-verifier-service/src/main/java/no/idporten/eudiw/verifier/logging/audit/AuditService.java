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

    public AuditService(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void auditStartVerificationRequest(StartVerificationRequest startVerificationRequest, ClientApplication clientApplication, String verifierTransactionId, String requestId) {

        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.START_VERIFICATION_REQUEST.auditIdentifier())
                .attribute("verifier_transaction_id", verifierTransactionId)
                .attribute("request_id", requestId)
                .attribute("client_application_id", clientApplication.getId())
                .attribute("dcql_query", startVerificationRequest.dcqlQuery())
                .attribute("redirect_uri", startVerificationRequest.redirectUri().toString())
                .build();

        auditLogger.log(auditEntry);
    }
}
