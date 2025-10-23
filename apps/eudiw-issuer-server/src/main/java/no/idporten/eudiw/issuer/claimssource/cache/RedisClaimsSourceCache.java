package no.idporten.eudiw.issuer.claimssource.cache;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.cache.RedisCache;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
    public void storeClaims(IssuanceTransactionId transactionId, Map<String, String> claims) {
        // TODO egen sak på cache levetid, må kunne styres mot oauth2 server
        cache.put(cacheKey(transactionId.getValue()), claims, Duration.of(30, ChronoUnit.MINUTES));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> retrieveClaims(IssuanceTransactionId transactionId) {
        return (Map<String, String>) cache.remove(cacheKey(transactionId.getValue()));
    }

}
