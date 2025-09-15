package no.idporten.eudiw.issuer.openid4vci.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service tracking notifications during credential issuance.
 */
@Service
public class NotificationService {

    // TODO cache på ordentlig måte
    // notification_id -> status - used for updates
    private final Map<NotificationId, String> notifications = new HashMap<>();

    // issuer transaction id -> notification_id - used for polling
    private final Map<IssuerTransactionId, NotificationId> pollNotifications = new HashMap<>();

    /**
     * Creates a notification id linked to issuer transaction id and sets initial status.
     */
    public void offerIssued(IssuerTransactionId issuerTransactionId) {
        NotificationId notificationId = new NotificationId();
        pollNotifications.put(issuerTransactionId, notificationId);
        issuerStatusUpdated(notificationId, "offer_issued");
    }

    /**
     * Updates status if notification is possible for issuer transaction.  Only pre-authorized code flow is tracked at the moment.
     */
    public NotificationId credentialIssued(IssuerTransactionId issuerTransactionId) {
        NotificationId notificationId = pollNotifications.get(issuerTransactionId);
        if (notificationId == null) {
            return null;
        }
        issuerStatusUpdated(notificationId, "credential_issued");
        return notificationId;
    }

    /**
     * Updates status from issuer events.
     */
    private void issuerStatusUpdated(NotificationId notificationId, String status) {
        notifications.put(notificationId, status);
    }

    /**
     * Updates status from wallet events.
     */
    public void walletStatusUpdated(NotificationId notificationId, String status) {
        notifications.put(notificationId, status);
    }

    /**
     * Polls status using issuer transaction id.  Status is unknown if is not tracked.
     */
    public String pollIssuerStatus(IssuerTransactionId issuerTransactionId) {
        NotificationId notificationId = pollNotifications.get(issuerTransactionId);
        return notifications.getOrDefault(notificationId, "unknown");
    }

}
