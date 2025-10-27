package no.idporten.eudiw.issuer.claimssource.cache;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.cache.RedisCache;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisClaimsSourceCache implements ClaimsSourceCache {

    @Value("${spring.application.name}")
    private String applicationName;

    private final RedisCache cache;

    protected String cacheKey(String key) {
        return applicationName + ":issuance:data:" + key;
    }

    @Override
    public void storeClaims(IssuanceTransactionId transactionId, Map<String, String> claims, Duration lifetime) {
        cache.put(cacheKey(transactionId.getValue()), claims, lifetime);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> retrieveClaims(IssuanceTransactionId transactionId) {
        return (Map<String, String>) cache.remove(cacheKey(transactionId.getValue()));
    }

}
