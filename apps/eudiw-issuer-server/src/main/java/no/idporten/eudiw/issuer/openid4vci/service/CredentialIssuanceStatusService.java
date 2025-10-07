package no.idporten.eudiw.issuer.openid4vci.service;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service tracking issuance status during credential issuance.
 */
@Service
public class CredentialIssuanceStatusService {

    private final static Logger log = LoggerFactory.getLogger(CredentialIssuanceStatusService.class);

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final AccessTokenValidationService accessTokenValidationService;
    private final AuditService auditService;

    public CredentialIssuanceStatusService(
            CredentialIssuerServerProperties credentialIssuerServerProperties,
            AccessTokenValidationService accessTokenValidationService, AuditService auditService) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.accessTokenValidationService = accessTokenValidationService;
        this.auditService = auditService;
    }

    // TODO cache issuance state med expiry
    private final Map<IssuanceTransactionId, IssuanceStatus> issuanceStatusCache = new HashMap<>();
    private final Map<NotificationId, IssuanceTransactionId> notificationIdToIssuanceTransactionIdCache = new HashMap<>();

    /**
     * Sets and returns initial credential issuance status.
     */
    public IssuanceStatus offerIssued(IssuanceTransactionId issuanceTransactionId, String credentialConfigurationId) {
        IssuanceStatus issuanceStatus = new IssuanceStatus(issuanceTransactionId, credentialConfigurationId, "offer_issued");
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
        IssuanceStatus issuanceStatus = new IssuanceStatus(issuanceTransactionId, credentialConfigurationId, "credential_issued");
        issuerStatusUpdated(issuanceTransactionId, issuanceStatus);
        NotificationId notificationId = new NotificationId();
        notificationIdToIssuanceTransactionIdCache.put(notificationId, issuanceTransactionId);
        return notificationId;
    }

    /**
     * Get issuance status using issuance transaction id.  Status is unknown if is not tracked.
     */
    public IssuanceStatus getIssuanceStatus(JWT accessToken, IssuanceTransactionId issuanceTransactionId) {
        IssuanceStatus issuanceStatus = issuanceStatusCache.get(issuanceTransactionId);
        if (issuanceStatus == null) {
            log.info("No issuance status found for issuance_transaction_id {}", issuanceTransactionId);
            return new IssuanceStatus(issuanceTransactionId, null, "unknown");
        }
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(issuanceStatus.credentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getPreAuthorizationServer(), credentialConfigurationProperties.getScope());
        return issuanceStatus;
    }

    public void issuerStatusUpdated(IssuanceTransactionId issuanceTransactionId, IssuanceStatus issuanceStatus) {
        issuanceStatusCache.put(issuanceTransactionId, issuanceStatus);
        log.info("Recorded issuance status {} for issuance_transaction_id {}", issuanceStatus.status(), issuanceTransactionId);
    }

    /**
     * Updates status from wallet events.
     */
    public void walletStatusUpdated(NotificationId notificationId, String status) {
        IssuanceTransactionId issuanceTransactionId = notificationIdToIssuanceTransactionIdCache.get(notificationId);
        if (issuanceTransactionId == null) {
            log.info("No issuance transaction id found for notification_id {}, ignoring status {}", notificationId, status);
            return;
        }
        IssuanceStatus issuanceStatus = issuanceStatusCache.get(issuanceTransactionId);
        if (issuanceStatus == null) {
            log.info("No issuance status found for issuance_transaction_id {}, ignoring status {}", issuanceTransactionId, status);
            return;
        }
        issuanceStatusCache.put(issuanceTransactionId, new IssuanceStatus(issuanceTransactionId, issuanceStatus.credentialConfigurationId(), status));
        log.info("Recorded issuance status {} for issuance_transaction_id {} from wallet with notification id {}", issuanceStatus.status(), issuanceTransactionId, notificationId);
        auditService.logWalletStatusUpdate(issuanceStatus.credentialConfigurationId(), issuanceTransactionId, notificationId, status);
    }

}
