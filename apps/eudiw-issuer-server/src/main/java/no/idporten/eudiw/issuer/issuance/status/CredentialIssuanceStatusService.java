package no.idporten.eudiw.issuer.issuance.status;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
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

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final CredentialConfigurationService credentialConfigurationService;
    private final CredentialIssuanceStatusCache credentialIssuanceStatusCache;
    private final AccessTokenValidationService accessTokenValidationService;
    private final AuditService auditService;

    public CredentialIssuanceStatusService(
            CredentialIssuerServerProperties credentialIssuerServerProperties, CredentialConfigurationService credentialConfigurationService, CredentialIssuanceStatusCache credentialIssuanceStatusCache,
            AccessTokenValidationService accessTokenValidationService, AuditService auditService) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.credentialConfigurationService = credentialConfigurationService;
        this.credentialIssuanceStatusCache = credentialIssuanceStatusCache;
        this.accessTokenValidationService = accessTokenValidationService;
        this.auditService = auditService;
    }

    /**
     * Sets and returns initial credential issuance status.
     */
    public CredentialIssuanceStatus offerIssued(IssuanceTransactionId issuanceTransactionId, String credentialConfigurationId) {
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
        CredentialIssuanceStatus issuanceStatus = new CredentialIssuanceStatus(issuanceTransactionId, credentialConfigurationId, "credential_issued");
        issuerStatusUpdated(issuanceTransactionId, issuanceStatus);
        NotificationId notificationId = new NotificationId();
        credentialIssuanceStatusCache.connect(notificationId, issuanceTransactionId, credentialIssuerServerProperties.getIssuanceStatusPollingLifetime());
        return notificationId;
    }

    /**
     * Get issuance status using issuance transaction id.  Status is unknown if is not tracked.
     */
    public CredentialIssuanceStatus getIssuanceStatus(JWT accessToken, IssuanceTransactionId issuanceTransactionId) {
        CredentialIssuanceStatus issuanceStatus = credentialIssuanceStatusCache.retrieveStatus(issuanceTransactionId);
        if (issuanceStatus == null) {
            log.info("No issuance status found for issuance_transaction_id {}", issuanceTransactionId);
            return new CredentialIssuanceStatus(issuanceTransactionId, null, "unknown");
        }
        CredentialConfigurationProperties credentialConfigurationProperties = credentialConfigurationService.findCredentialConfiguration(issuanceStatus.credentialConfigurationId());
         accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getPreAuthorizationServer(), credentialConfigurationProperties.getScope());
        return issuanceStatus;
    }

    public void issuerStatusUpdated(IssuanceTransactionId issuanceTransactionId, CredentialIssuanceStatus issuanceStatus) {
        credentialIssuanceStatusCache.updateStatus(issuanceTransactionId, issuanceStatus, credentialIssuerServerProperties.getIssuanceStatusPollingLifetime());
        log.info("Recorded issuance status {} for issuance_transaction_id {}", issuanceStatus.status(), issuanceTransactionId);
    }

    /**
     * Updates status from wallet events.
     */
    public void walletStatusUpdated(NotificationId notificationId, String status) {
        IssuanceTransactionId issuanceTransactionId = credentialIssuanceStatusCache.lookup(notificationId);
        if (issuanceTransactionId == null) {
            log.info("No issuance transaction id found for notification_id {}, ignoring status {}", notificationId, status);
            return;
        }
        CredentialIssuanceStatus issuanceStatus = credentialIssuanceStatusCache.retrieveStatus(issuanceTransactionId);
        if (issuanceStatus == null) {
            log.info("No issuance status found for issuance_transaction_id {}, ignoring status {}", issuanceTransactionId, status);
            return;
        }
        credentialIssuanceStatusCache.updateStatus(issuanceTransactionId, new CredentialIssuanceStatus(issuanceTransactionId, issuanceStatus.credentialConfigurationId(), status), credentialIssuerServerProperties.getIssuanceStatusPollingLifetime());
        log.info("Recorded issuance status {} for issuance_transaction_id {} from wallet with notification id {}", issuanceStatus.status(), issuanceTransactionId, notificationId);
        auditService.logWalletStatusUpdate(issuanceStatus.credentialConfigurationId(), issuanceTransactionId, notificationId, status);
    }

}
