package no.idporten.eudiw.issuer.issuance.status;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.cache.RedisCache;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisCredentialIssuanceStatusCache implements CredentialIssuanceStatusCache {

    @Value("${spring.application.name}")
    private String applicationName;

    private final RedisCache cache;

    protected String cacheKey(IssuanceTransactionId issuanceTransactionId) {
        return applicationName + ":issuance:status:" + issuanceTransactionId.getValue();
    }

    protected String cacheKey(NotificationId notificationId) {
        return applicationName + ":issuance:notification:" + notificationId.getValue();
    }

    @Override
    public void updateStatus(IssuanceTransactionId issuanceTransactionId, CredentialIssuanceStatus issuanceStatus, Duration duration) {
        cache.put(cacheKey(issuanceTransactionId), issuanceStatus, duration);
    }

    @Override
    public CredentialIssuanceStatus retrieveStatus(IssuanceTransactionId issuanceTransactionId) {
        return (CredentialIssuanceStatus) cache.get(cacheKey(issuanceTransactionId));
    }

    @Override
    public void connect(NotificationId notificationId, IssuanceTransactionId issuanceTransactionId, Duration duration) {
        cache.put(cacheKey(notificationId), issuanceTransactionId, duration);
    }

    @Override
    public IssuanceTransactionId lookup(NotificationId notificationId) {
        return (IssuanceTransactionId) cache.get(cacheKey(notificationId));
    }

}
