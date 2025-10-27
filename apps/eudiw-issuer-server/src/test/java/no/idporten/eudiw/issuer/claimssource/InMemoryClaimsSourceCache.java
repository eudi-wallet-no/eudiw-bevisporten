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

    private final Map<String, Map<String, String>> cache = new HashMap<>();

    @Override
    public void storeClaims(IssuanceTransactionId transactionId, Map<String, String> claims, Duration ignoredLifetime) {
        cache.put(transactionId.getValue(), claims);
    }

    @Override
    public Map<String, String> retrieveClaims(IssuanceTransactionId transactionId) {
        return cache.remove(transactionId.getValue());
    }

}
