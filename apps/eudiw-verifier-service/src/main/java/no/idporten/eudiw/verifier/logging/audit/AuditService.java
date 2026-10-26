package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.eudiw.verifier.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.api.verification.StartVerificationResponse;
import no.idporten.eudiw.verifier.api.verification.VerificationResultResponse;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.VerificationTransaction;
import no.idporten.eudiw.verifier.openid4vp.VpToken;
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
    protected static final String AUTHORIZATION_REQUEST = "authorization_request";
    protected static final String AUTHORIZATION_REQUEST_QR_CODE = "authorization_request_qr_code";
    protected static final String AUTHORIZATION_RESPONSE = "authorization_response";
    protected static final String CREDENTIALS = "credentials";
    protected static final String FLOW = "flow";
    protected static final String VP_TOKEN = "vp_token";
    protected static final String STATE = "state";

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

    public void auditStartVerificationRequestResponse(StartVerificationResponse startVerificationResponse, ClientApplication clientApplication, String requestId) {

        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.START_VERIFICATION_RESPONSE.auditIdentifier())
                .attribute(VERIFIER_TRANSACTION_ID, startVerificationResponse.verifierTransactionId())
                .attribute(REQUEST_ID, requestId)
                .attribute(CLIENT_APPLICATION_ID, clientApplication.getId())
                .attribute(AUTHORIZATION_REQUEST, startVerificationResponse.authorizationRequest().toString())
                .attribute(AUTHORIZATION_REQUEST_QR_CODE, startVerificationResponse.authorizationRequestQRCode().toString())
                .build();
        auditLogger.log(auditEntry);

    }

    public void auditClientFetchVerificationDataRequest(VerificationTransaction verificationTransaction, String verifierTransactionId, ClientApplication clientApplication) {
        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.RETRIEVE_VERIFICATION_DATA_REQUEST.auditIdentifier())
                .attribute(VERIFIER_TRANSACTION_ID, verifierTransactionId)
                .attribute(CLIENT_APPLICATION_ID, clientApplication.getId())
                .attribute(DCQL_QUERY, verificationTransaction.getDcqlQuery())
                .attribute(REDIRECT_URI, verificationTransaction.getRedirectUri().toString())
                .build();
        auditLogger.log(auditEntry);
    }

    public void auditClientFetchVerificationDataResponse(VerificationResultResponse verificationResultResponse, String verifierTransactionId, ClientApplication clientApplication) {
        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.RETRIEVE_VERIFICATION_DATA_RESPONSE.auditIdentifier())
                .attribute(VERIFIER_TRANSACTION_ID, verifierTransactionId)
                .attribute(CLIENT_APPLICATION_ID, clientApplication.getId())
                .attribute(CREDENTIALS, verificationResultResponse.credentials())
                .attribute(AUTHORIZATION_RESPONSE, verificationResultResponse.authorizationResponse())
                .build();
        auditLogger.log(auditEntry);
    }

    public void auditFetchAuthzRequestStart(String verifierTransactionId, ClientApplication clientApplication, String requestId, String flow) {
        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.RETRIEVE_AUTHZ_REQUEST.auditIdentifier())
                .attribute(VERIFIER_TRANSACTION_ID, verifierTransactionId)
                .attribute(REQUEST_ID, requestId)
                .attribute(CLIENT_APPLICATION_ID, clientApplication.getId())
                .attribute(FLOW, flow)
                .build();
        auditLogger.log(auditEntry);

    }

    public void auditFetchAuthzRequestEnd(VerificationTransaction verificationTransaction, String verifierTransactionId, ClientApplication clientApplication, String requestId, String authzRequestJwt, String state) {
        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.SEND_AUTHZ_REQUEST.auditIdentifier())
                .attribute(VERIFIER_TRANSACTION_ID, verifierTransactionId)
                .attribute(REQUEST_ID, requestId)
                .attribute(CLIENT_APPLICATION_ID, clientApplication.getId())
                .attribute(FLOW, verificationTransaction.getFlow())
                .attribute(AUTHORIZATION_REQUEST, authzRequestJwt)
                .attribute(STATE, state)
                .build();
        auditLogger.log(auditEntry);
    }

    public void auditFetchDecryptedVpTokenAuthzResponse(VpToken vpToken, String verifierTransactionId, ClientApplication clientApplication, String state) {
        AuditEntry auditEntry = AuditEntry.builder()
                .auditId(AuditID.RECEIVE_AUTHZ_RESPONSE.auditIdentifier())
                .attribute(VERIFIER_TRANSACTION_ID, verifierTransactionId)
                .attribute(CLIENT_APPLICATION_ID, clientApplication.getId())
                .attribute(VP_TOKEN, vpToken.toJson())
                .attribute(STATE, state)
                .build();
        auditLogger.log(auditEntry);
    }
}
