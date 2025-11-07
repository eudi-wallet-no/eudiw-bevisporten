package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Claims source cache storing data in memory for test classes.
 */
public class InMemoryClaimsSourceCache implements ClaimsSourceCache {

    private final Map<String, Map<String, Object>> cache = new HashMap<>();

    @Override
    public void storeClaims(IssuanceTransactionId transactionId, Map<String, Object> claims, Duration ignoredLifetime) {
        HashMap<String, Object> claimsCopy = new HashMap<>(claims);
        claimsCopy.put("@class", "TestThatJacksonSerializationIsIgnored");
        cache.put(transactionId.getValue(), claimsCopy);
    }

    @Override
    public Map<String, Object> retrieveClaims(IssuanceTransactionId transactionId) {
        return cache.remove(transactionId.getValue());
    }

}
