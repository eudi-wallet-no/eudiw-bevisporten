package no.idporten.eudiw.issuer.logging.audit;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    // Attributes for audit logging
    protected static final String CREDENTIAL_CONFIGURATION_ID = "credential_configuration_id";
    protected static final String ACCESS_TOKEN = "access_token";
    protected static final String CREDENTIAL_ISSUER = "credential_issuer";
    protected static final String AUTHORIZATION_SERVER = "authorization_server";
    protected static final String ISSUANCE_TRANSACTION_ID = "issuance_transaction_id";
    protected static final String NOTIFICATION_ID = "notification_id";
    protected static final String FORMAT = "format";
    protected static final String STATUS = "status";
    protected static final String INSTANCES = "instances";
    protected static final String SUBJECT = "subject";

    @Qualifier("auditLogger")
    private final AuditLogger auditLogger;


    public AuditService(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void logStartCredentialIssuanceTransaction(@NotEmpty String credentialIssuer, @NotEmpty String credentialConfigurationId, IssuanceTransactionId issuanceTransactionId, @NotNull JWT accessToken) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.START_CREDENTIAL_ISSUANCE.auditIdentifier())
                .logNullAttributes(false)
                .attribute(CREDENTIAL_CONFIGURATION_ID, credentialConfigurationId)
                .attribute(CREDENTIAL_ISSUER, credentialIssuer)
                .attribute(ISSUANCE_TRANSACTION_ID, identifierValue(issuanceTransactionId))
                .attribute(ACCESS_TOKEN, maskJWT(accessToken))
                .build());
    }

    public void logCreateCredentialOffer(@NotEmpty String credentialIssuer, @NotEmpty List<String> credentialConfigurationId) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.CREATE_CREDENTIAL_OFFER.auditIdentifier())
                .logNullAttributes(false)
                .attribute(CREDENTIAL_CONFIGURATION_ID, credentialConfigurationId)
                .attribute(CREDENTIAL_ISSUER, credentialIssuer)
                .build());
    }

    public void logIssueCredentials(CredentialIssueContext context, IssuanceTransactionId issuanceTransactionId, int instances, NotificationId notificationId) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.ISSUE_CREDENTIAL.auditIdentifier())
                .logNullAttributes(false)
                .attribute(SUBJECT, context.personIdentifier())
                .attribute(CREDENTIAL_CONFIGURATION_ID, context.credentialConfiguration().getCredentialConfigurationId())
                .attribute(AUTHORIZATION_SERVER, context.credentialConfiguration().getCredentialIssuerContext().getAuthorizationServer())
                .attribute(FORMAT, context.credentialConfiguration().getFormat().formatIdentifier())
                .attribute(INSTANCES, instances)
                .attribute(NOTIFICATION_ID, identifierValue(notificationId))
                .attribute(ACCESS_TOKEN, maskJWT(context.accessToken()))
                .attribute(ISSUANCE_TRANSACTION_ID, identifierValue(issuanceTransactionId))
                .build());
    }

    public void logWalletStatusUpdate(String credentialConfigurationId, IssuanceTransactionId issuanceTransactionId, NotificationId notificationId, String status) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.WALLET_UPDATED_STATUS.auditIdentifier())
                .logNullAttributes(false)
                .attribute(CREDENTIAL_CONFIGURATION_ID, credentialConfigurationId)
                .attribute(ISSUANCE_TRANSACTION_ID, identifierValue(issuanceTransactionId))
                .attribute(NOTIFICATION_ID, identifierValue(notificationId))
                .attribute(STATUS, status)
                .build());
    }

    protected static String identifierValue(Identifier identifier) {
        return identifier != null ? identifier.getValue() : null;
    }

    protected static String maskJWT(JWT accessToken) {
        if (accessToken == null || accessToken.getParsedParts() == null || accessToken.getParsedParts().length < 2) {
            throw new IssuerServerException(ErrorCode.INVALID_CREDENTIAL_REQUEST,"Invalid token");
        }
        Base64URL[] parsedParts = accessToken.getParsedParts();
        return "%s.%s.".formatted(parsedParts[0].toString(), parsedParts[1].toString());
    }

}
