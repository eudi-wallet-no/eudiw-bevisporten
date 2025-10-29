package no.idporten.eudiw.issuer.issuance.status;

import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;

import java.time.Duration;

/**
 * Cache for status updates during credential issuance.
 */
public interface CredentialIssuanceStatusCache {

    /**
     * Track status of credential issuer transaction.
     */
    void updateStatus(IssuanceTransactionId issuanceTransactionId, CredentialIssuanceStatus issuanceStatus, Duration duration);

    /**
     * Retrieve status of credential issuer transaction.
     */
    CredentialIssuanceStatus retrieveStatus(IssuanceTransactionId issuanceTransactionId);

    /**
     * Connect a wallet notification id with a credential issuer transaction id.
     */
    void connect(NotificationId notificationId, IssuanceTransactionId issuanceTransactionId, Duration duration);

    /**
     * Look up issuer transaction id.  Use result to retrieve status and update status.
     */
    IssuanceTransactionId lookup(NotificationId notificationId);

}
