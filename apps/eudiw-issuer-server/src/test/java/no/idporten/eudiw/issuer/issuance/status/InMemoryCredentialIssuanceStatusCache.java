package no.idporten.eudiw.issuer.issuance.status;

import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class InMemoryCredentialIssuanceStatusCache implements CredentialIssuanceStatusCache {

    private final Map<IssuanceTransactionId, CredentialIssuanceStatus> statusMap = new HashMap<>();
    private final Map<NotificationId, IssuanceTransactionId> keyMap = new HashMap<>();

    @Override
    public void updateStatus(IssuanceTransactionId issuanceTransactionId, CredentialIssuanceStatus issuanceStatus, Duration duration) {
        statusMap.put(issuanceTransactionId, issuanceStatus);
    }

    @Override
    public CredentialIssuanceStatus retrieveStatus(IssuanceTransactionId issuanceTransactionId) {
        return statusMap.get(issuanceTransactionId);
    }

    @Override
    public void connect(NotificationId notificationId, IssuanceTransactionId issuanceTransactionId, Duration duration) {
        keyMap.put(notificationId, issuanceTransactionId);
    }

    @Override
    public IssuanceTransactionId lookup(NotificationId notificationId) {
        return keyMap.get(notificationId);
    }

}
