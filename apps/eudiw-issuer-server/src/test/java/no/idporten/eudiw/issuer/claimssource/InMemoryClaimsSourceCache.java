package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
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
    public void storeClaims(CredentialIssuerTenant credentialIssuer, IssuanceTransactionId transactionId, Map<String, Object> claims, Duration lifetime) {
        cache.put(cacheKey("memory", credentialIssuer, transactionId), claims);
    }

    @Override
    public Map<String, Object> retrieveClaims(CredentialIssuerTenant credentialIssuer, IssuanceTransactionId transactionId) {
        return cache.remove(cacheKey("memory", credentialIssuer, transactionId));
    }

}
