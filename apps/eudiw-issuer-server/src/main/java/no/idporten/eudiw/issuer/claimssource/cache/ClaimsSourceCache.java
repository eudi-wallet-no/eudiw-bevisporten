package no.idporten.eudiw.issuer.claimssource.cache;

import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

import java.time.Duration;
import java.util.Map;

/**
 * Data cache for claims sources during issuance.
 */
public interface ClaimsSourceCache {

    /**
     * Stores claims in cache for a lifetime.  The claims are removed from cache when retrieved or when the lifetime expires.
     */
    void storeClaims(IssuanceTransactionId transactionId, Map<String, Object> claims, Duration lifetime);

    /**
     * Retrieves and deletes claims from cache.
     */
    Map<String, Object> retrieveClaims(IssuanceTransactionId transactionId);

}
