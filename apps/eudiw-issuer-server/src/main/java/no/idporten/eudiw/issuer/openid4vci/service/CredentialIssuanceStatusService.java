package no.idporten.eudiw.issuer.openid4vci.service;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
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

    public CredentialIssuanceStatusService(
            CredentialIssuerServerProperties credentialIssuerServerProperties,
            AccessTokenValidationService accessTokenValidationService) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.accessTokenValidationService = accessTokenValidationService;
    }

    // TODO cache issuance state med expiry
    private final Map<IssuerTransactionId, IssuanceStatus> issuanceStatusCache = new HashMap<>();
    private final Map<NotificationId, IssuerTransactionId> notificationIdToIssuerTransactionIdCache = new HashMap<>();

    @SneakyThrows
    private IssuerTransactionId getIssuerTransactionId(JWT accessToken) {
        if (accessToken.getJWTClaimsSet().getStringClaim("tx_id") != null) {
            return new IssuerTransactionId(accessToken.getJWTClaimsSet().getStringClaim("tx_id"));
        }
        return null;
    }

    /**
     * Sets and returns initial credential issuance status.
     */
    public IssuanceStatus offerIssued(IssuerTransactionId issuerTransactionId, String credentialConfigurationId) {
        IssuanceStatus issuanceStatus = new IssuanceStatus(issuerTransactionId, credentialConfigurationId, "offer_issued");
        issuerStatusUpdated(issuerTransactionId, issuanceStatus);
        return issuanceStatus;
    }

    /**
     * Sets or creates credential issuance status and creates notification id for wallet.
     */
    public NotificationId credentialIssued(JWT accessToken, String credentialConfigurationId) {
        IssuerTransactionId issuerTransactionId = getIssuerTransactionId(accessToken);
        if (issuerTransactionId == null) {
            return null;
        }
        IssuanceStatus issuanceStatus = new IssuanceStatus(issuerTransactionId, credentialConfigurationId, "credential_issued");
        issuerStatusUpdated(issuerTransactionId, issuanceStatus);
        NotificationId notificationId = new NotificationId();
        notificationIdToIssuerTransactionIdCache.put(notificationId, issuerTransactionId);
        return notificationId;
    }

    /**
     * Polls status using issuer transaction id.  Status is unknown if is not tracked.
     */
    public IssuanceStatus pollIssuerStatus(JWT accessToken, IssuerTransactionId issuerTransactionId) {
        IssuanceStatus issuanceStatus = issuanceStatusCache.get(issuerTransactionId);
        if (issuanceStatus == null) {
            log.info("No issuance status found for issuer_transaction_id {}", issuerTransactionId);
            return new IssuanceStatus(issuerTransactionId, null, "unknown");
        }
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(issuanceStatus.credentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getPreAuthorizationServer(), credentialConfigurationProperties.getScope());
        return issuanceStatus;
    }

    public void issuerStatusUpdated(IssuerTransactionId issuerTransactionId, IssuanceStatus issuanceStatus) {
        issuanceStatusCache.put(issuerTransactionId, issuanceStatus);
        log.info("Recorded issuance status {} for issuer transaction id {}", issuanceStatus.status(), issuerTransactionId);
    }

    /**
     * Updates status from wallet events.
     */
    public void walletStatusUpdated(NotificationId notificationId, String status) {
        IssuerTransactionId issuerTransactionId = notificationIdToIssuerTransactionIdCache.get(notificationId);
        if (issuerTransactionId == null) {
            log.info("No issuer transaction id found for notification_id {}, ignoring status {}", notificationId, status);
            return;
        }
        IssuanceStatus issuanceStatus = issuanceStatusCache.get(issuerTransactionId);
        if (issuanceStatus == null) {
            log.info("No issuance status found for issuer_transaction_id {}, ignoring status {}", issuerTransactionId, status);
            return;
        }
        issuanceStatusCache.put(issuerTransactionId, new IssuanceStatus(issuerTransactionId, issuanceStatus.credentialConfigurationId(), status));
        log.info("Recorded issuance status {} for issuer transaction id {} from wallet with notification id {}", issuanceStatus.status(), issuerTransactionId, notificationId);
    }

}
