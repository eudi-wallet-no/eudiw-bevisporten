package no.idporten.eudiw.issuer.issuance.status;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionEntity;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionDao;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenCredentialValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service tracking issuance status during credential issuance.
 */
@Service
public class CredentialIssuanceStatusService {

    private final static Logger log = LoggerFactory.getLogger(CredentialIssuanceStatusService.class);

    private final CredentialIssuanceTransactionDao issuanceTransactionDao;
    private final AccessTokenValidationService accessTokenValidationService;
    private final AuditService auditService;

    public CredentialIssuanceStatusService(CredentialIssuanceTransactionDao issuanceTransactionDao, AccessTokenValidationService accessTokenValidationService, AuditService auditService) {
        this.issuanceTransactionDao = issuanceTransactionDao;
        this.accessTokenValidationService = accessTokenValidationService;
        this.auditService = auditService;
    }

    /**
     * Sets and returns initial credential issuance status.
     */
    public CredentialIssuanceStatus offerIssued(CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId issuanceTransactionId, String credentialConfigurationId) {
        issuanceTransactionDao.insertTransaction(issuanceTransactionId.getValue(), credentialConfigurationId, credentialIssuerTenant.getId(), System.currentTimeMillis());
        CredentialIssuanceStatus issuanceStatus = new CredentialIssuanceStatus(issuanceTransactionId, credentialConfigurationId, "offer_issued");
        issuerStatusUpdated(issuanceTransactionId, issuanceStatus);
        return issuanceStatus;
    }

    /**
     * Sets or creates credential issuance status and creates notification id for wallet.
     */
    public NotificationId credentialIssued(String credentialConfigurationId, IssuanceTransactionId issuanceTransactionId) {
        if (issuanceTransactionId == null) {
            return null;
        }
        issuerStatusUpdated(issuanceTransactionId, new CredentialIssuanceStatus(issuanceTransactionId, credentialConfigurationId, "credential_issued"));
        NotificationId notificationId = new NotificationId();
        issuanceTransactionDao.updateNotificationId(issuanceTransactionId.getValue(), notificationId.getValue(), System.currentTimeMillis());
        return notificationId;
    }

    /**
     * Get issuance status using issuance transaction id. Status is unknown if not tracked.
     */
    public CredentialIssuanceStatus getIssuanceStatus(JWT accessToken, CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId issuanceTransactionId) {
        return issuanceTransactionDao.findByIssuanceTransactionId(issuanceTransactionId.getValue(), credentialIssuerTenant.getId())
                .filter(entity -> entity.getStatus() != null)
                .map(entity -> {
                    ExtendedCredentialConfiguration credentialConfiguration = credentialIssuerTenant.findCredentialConfiguration(entity.getCredentialConfigurationId());
                    accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, AccessTokenCredentialValidationContext.forPreAuthorization(credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationServer(), credentialConfiguration.getScope()));
                    return new CredentialIssuanceStatus(issuanceTransactionId, entity.getCredentialConfigurationId(), entity.getStatus());
                })
                .orElseGet(() -> {
                    log.info("No issuance status found for issuance_transaction_id {}", issuanceTransactionId);
                    return new CredentialIssuanceStatus(issuanceTransactionId, null, "unknown");
                });
    }

    /**
     * Updates status from issuer-side events.
     */
    public void issuerStatusUpdated(IssuanceTransactionId issuanceTransactionId, CredentialIssuanceStatus issuanceStatus) {
        issuanceTransactionDao.updateStatus(issuanceTransactionId.getValue(), issuanceStatus.status(), System.currentTimeMillis());
        log.info("Recorded issuance status {} for issuance_transaction_id {}", issuanceStatus.status(), issuanceTransactionId);
    }

    /**
     * Updates status from wallet notification.  Validates access token against the actual credential configuration.
     */
    public void walletStatusUpdated(CredentialIssuerTenant credentialIssuerTenant, NotificationId notificationId, String status, JWT accessToken) {
        CredentialIssuanceTransactionEntity entity = issuanceTransactionDao.findByNotificationId(notificationId.getValue()).orElse(null);
        if (entity == null) {
            throw new IssuerServerException(ErrorCode.INVALID_NOTIFICATION_ID, "Unknown notification id.", "Unknown notification id %s for tenant %s with status %s.".formatted(notificationId, credentialIssuerTenant.getId(), status));
        }
        if (!credentialIssuerTenant.getId().equals(entity.getCredentialIssuerTenant())) {
            throw new IssuerServerException(ErrorCode.INVALID_NOTIFICATION_ID, "Notification id does not belong to credential issuer tenant.", "Unknown notification id %s for tenant %s with status %s.".formatted(notificationId, credentialIssuerTenant.getId(), status));
        }
        ExtendedCredentialConfiguration credentialConfiguration = credentialIssuerTenant.findCredentialConfiguration(entity.getCredentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, AccessTokenCredentialValidationContext.forAuthorization(credentialConfiguration));
        issuanceTransactionDao.updateStatus(entity.getIssuanceTransactionId(), status, System.currentTimeMillis());
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId(entity.getIssuanceTransactionId());
        log.info("Recorded issuance status {} for issuance_transaction_id {} from wallet with notification id {}", status, issuanceTransactionId, notificationId);
        auditService.logWalletStatusUpdate(entity.getCredentialConfigurationId(), issuanceTransactionId, notificationId, status);
    }

}
