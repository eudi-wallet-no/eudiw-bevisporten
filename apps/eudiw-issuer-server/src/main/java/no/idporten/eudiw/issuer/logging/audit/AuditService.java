package no.idporten.eudiw.issuer.logging.audit;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.service.NotificationId;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    public void logCreateCredentialOffer(@NotEmpty String credentialIssuer, @NotEmpty String credentialConfigurationId) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.CREATE_CREDENTIAL_OFFER.auditIdentifier())
                .logNullAttributes(false)
                .attribute(CREDENTIAL_CONFIGURATION_ID, credentialConfigurationId)
                .attribute(CREDENTIAL_ISSUER, credentialIssuer)
                .build());
    }

    public void logIssueCredentials(@NotNull String authorizationServer, @NotEmpty String credentialConfigurationId, IssuanceTransactionId issuanceTransactionId, @NotNull CredentialFormat format, NotificationId notificationId, @NotNull JWT accessToken) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditID.ISSUE_CREDENTIAL.auditIdentifier())
                .logNullAttributes(false)
                .attribute(CREDENTIAL_CONFIGURATION_ID, credentialConfigurationId)
                .attribute(AUTHORIZATION_SERVER, authorizationServer)
                .attribute(FORMAT, format.formatIdentifier())
                .attribute(NOTIFICATION_ID, identifierValue(notificationId))
                .attribute(ACCESS_TOKEN, maskJWT(accessToken))
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
            throw new IssuerServerException(IssuerServerException.INVALID_CREDENTIAL_REQUEST,"Invalid token", HttpStatus.BAD_REQUEST);
        }
        Base64URL[] parsedParts = accessToken.getParsedParts();
        return "%s.%s.".formatted(parsedParts[0].toString(), parsedParts[1].toString());
    }

}
